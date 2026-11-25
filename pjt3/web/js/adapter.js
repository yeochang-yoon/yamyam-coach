'use strict';

function createStore(storage, sessionStorage, catalog, webCrypto) {
    var domain;
    if (typeof module !== 'undefined') {
        domain = require('./domain.js');
    } else {
        domain = YamyamDomain;
    }
    var stateKey = 'yamyam-pjt3-v2';
    var sessionKey = stateKey + '-user';

    function readState() {
        var saved = storage.getItem(stateKey);
        if (saved === null) {
            throw new Error('초기 식단을 불러오는 중입니다.');
        }
        try {
            var state = JSON.parse(saved);
            if (!state || !Array.isArray(state.users) || !Array.isArray(state.meals)
                || !Number.isSafeInteger(state.nextUserId) || !Number.isSafeInteger(state.nextMealId)) {
                throw new Error();
            }
            return state;
        } catch (error) {
            throw new Error('브라우저에 저장된 식단 데이터를 읽을 수 없습니다.');
        }
    }

    function nextId(records) {
        var id = 1;
        for (var i = 0; i < records.length; i++) {
            if (records[i].id >= id) {
                id = records[i].id + 1;
            }
        }
        return id;
    }

    function saveState(state) {
        storage.setItem(stateKey, JSON.stringify(state));
    }

    function initialize(users, meals) {
        if (storage.getItem(stateKey) !== null) {
            readState();
            return;
        }
        saveState({
            users: users,
            meals: meals,
            nextUserId: nextId(users),
            nextMealId: nextId(meals)
        });
    }

    function findUserByLogin(state, loginId) {
        for (var i = 0; i < state.users.length; i++) {
            if (state.users[i].loginId === loginId) {
                return state.users[i];
            }
        }
        return null;
    }

    function currentUser(state) {
        var id = sessionStorage.getItem(sessionKey);
        for (var i = 0; i < state.users.length; i++) {
            var user = state.users[i];
            if (String(user.id) === id && user.status === 'ACTIVE') {
                return user;
            }
        }
        return null;
    }

    function requireUser(state) {
        var user = currentUser(state);
        if (!user) {
            throw new Error('먼저 로그인하세요.');
        }
        return user;
    }

    function publicProfile(user) {
        return {
            id: user.id,
            loginId: user.loginId,
            name: user.name,
            heightCm: user.heightCm,
            weightKg: user.weightKg,
            conditions: user.conditions.slice()
        };
    }

    function profile() {
        var user = currentUser(readState());
        if (!user) {
            return null;
        }
        return publicProfile(user);
    }

    function toBase64(bytes) {
        var text = '';
        for (var i = 0; i < bytes.length; i++) {
            text += String.fromCharCode(bytes[i]);
        }
        return btoa(text);
    }

    function hashPassword(password, salt, iterations) {
        if (!webCrypto || !webCrypto.subtle) {
            throw new Error('비밀번호 기능을 사용하려면 localhost에서 페이지를 여세요.');
        }
        var rawSalt = atob(salt);
        var saltBytes = new Uint8Array(rawSalt.length);
        for (var i = 0; i < rawSalt.length; i++) {
            saltBytes[i] = rawSalt.charCodeAt(i);
        }
        var passwordBytes = new TextEncoder().encode(password);
        return webCrypto.subtle.importKey('raw', passwordBytes, 'PBKDF2', false, ['deriveBits'])
            .then(function (key) {
                return webCrypto.subtle.deriveBits({
                    name: 'PBKDF2',
                    hash: 'SHA-256',
                    salt: saltBytes,
                    iterations: iterations
                }, key, 256);
            })
            .then(function (bits) {
                return toBase64(new Uint8Array(bits));
            });
    }

    function register(loginId, password, details) {
        if (!/^[A-Za-z0-9_]{3,24}$/.test(loginId)) {
            throw new Error('아이디는 영문, 숫자, 밑줄 3~24자로 입력하세요.');
        }
        if (password.length < 8 || password.length > 128) {
            throw new Error('비밀번호는 8~128자로 입력하세요.');
        }
        domain.validateProfile(details);
        if (findUserByLogin(readState(), loginId)) {
            throw new Error('이미 사용 중인 아이디입니다.');
        }
        var salt = toBase64(webCrypto.getRandomValues(new Uint8Array(16)));
        var iterations = 600000;
        return hashPassword(password, salt, iterations).then(function (hash) {
            var state = readState();
            if (findUserByLogin(state, loginId)) {
                throw new Error('이미 사용 중인 아이디입니다.');
            }
            var user = {
                id: state.nextUserId++,
                loginId: loginId,
                name: details.name.trim(),
                heightCm: details.heightCm,
                weightKg: details.weightKg,
                conditions: details.conditions.slice(),
                status: 'ACTIVE',
                passwordHash: {
                    algorithm: 'PBKDF2WithHmacSHA256',
                    iterations: iterations,
                    salt: salt,
                    hash: hash
                }
            };
            state.users.push(user);
            saveState(state);
            return publicProfile(user);
        });
    }

    function login(loginId, password) {
        var user = findUserByLogin(readState(), loginId);
        var message = '아이디 또는 비밀번호를 확인하세요.';
        if (!user || user.status !== 'ACTIVE' || !password || password.length > 128) {
            throw new Error(message);
        }
        var saved = user.passwordHash;
        return hashPassword(password, saved.salt, saved.iterations).then(function (hash) {
            var latest = findUserByLogin(readState(), loginId);
            if (hash !== saved.hash || !latest || latest.id !== user.id || latest.status !== 'ACTIVE') {
                throw new Error(message);
            }
            sessionStorage.setItem(sessionKey, String(user.id));
            return publicProfile(latest);
        });
    }

    function logout() {
        sessionStorage.removeItem(sessionKey);
    }

    function updateProfile(details) {
        domain.validateProfile(details);
        var state = readState();
        var user = requireUser(state);
        user.name = details.name.trim();
        user.heightCm = details.heightCm;
        user.weightKg = details.weightKg;
        user.conditions = details.conditions.slice();
        saveState(state);
        return publicProfile(user);
    }

    function deleteAccount(password) {
        var user = requireUser(readState());
        var saved = user.passwordHash;
        return hashPassword(password, saved.salt, saved.iterations).then(function (hash) {
            if (hash !== saved.hash) {
                throw new Error('비밀번호를 확인하세요.');
            }
            var state = readState();
            if (requireUser(state).id !== user.id) {
                throw new Error('현재 로그인한 회원을 확인하세요.');
            }
            for (var i = state.users.length - 1; i >= 0; i--) {
                if (state.users[i].id === user.id) {
                    state.users.splice(i, 1);
                }
            }
            for (var j = state.meals.length - 1; j >= 0; j--) {
                if (state.meals[j].userId === user.id) {
                    state.meals.splice(j, 1);
                }
            }
            saveState(state);
            logout();
        });
    }

    function meals(date) {
        var state = readState();
        var user = requireUser(state);
        var matches = [];
        for (var i = 0; i < state.meals.length; i++) {
            var meal = state.meals[i];
            if (meal.userId === user.id && (!date || meal.date === date)) {
                matches.push(meal);
            }
        }
        matches.sort(function (a, b) {
            if (a.date !== b.date) {
                return b.date.localeCompare(a.date);
            }
            return a.id - b.id;
        });
        return matches;
    }

    function mealIndex(state, user, id) {
        for (var i = 0; i < state.meals.length; i++) {
            if (state.meals[i].id === id && state.meals[i].userId === user.id) {
                return i;
            }
        }
        throw new Error('식단을 찾을 수 없습니다.');
    }

    function meal(id) {
        var state = readState();
        var user = requireUser(state);
        return state.meals[mealIndex(state, user, id)];
    }

    function saveMeal(record) {
        var state = readState();
        var user = requireUser(state);
        domain.validateMeal(record, catalog);
        record.userId = user.id;
        if (record.id) {
            var index = mealIndex(state, user, record.id);
            state.meals[index] = record;
        } else {
            record.id = state.nextMealId++;
            state.meals.push(record);
        }
        saveState(state);
        return record;
    }

    function deleteMeal(id) {
        var state = readState();
        var user = requireUser(state);
        state.meals.splice(mealIndex(state, user, id), 1);
        saveState(state);
    }

    return {
        initialize: initialize,
        profile: profile,
        register: register,
        login: login,
        logout: logout,
        updateProfile: updateProfile,
        deleteAccount: deleteAccount,
        meals: meals,
        meal: meal,
        saveMeal: saveMeal,
        deleteMeal: deleteMeal
    };
}

if (typeof module !== 'undefined') {
    module.exports = createStore;
}
