'use strict';

var catalog;
var store;
var foodMatches = [];
var foodPage = 0;
var foodPageSize = 20;

function byId(id) {
    return document.getElementById(id);
}

function notice(message, isError) {
    var box = byId('notice');
    box.textContent = message;
    box.className = isError ? 'alert alert-danger' : 'alert alert-light';
    box.hidden = !message;
    if (isError) {
        box.scrollIntoView({block: 'nearest'});
    }
}

function showError(error) {
    notice(error.message, true);
}

function createTextElement(tag, text) {
    var element = document.createElement(tag);
    element.textContent = text;
    return element;
}

function todayText() {
    var date = new Date();
    var month = '0' + (date.getMonth() + 1);
    var day = '0' + date.getDate();
    return date.getFullYear() + '-' + month.slice(-2) + '-' + day.slice(-2);
}

function formatNumber(value) {
    return value.toLocaleString('ko-KR', {minimumFractionDigits: 1, maximumFractionDigits: 1});
}

function loadJson(path, callback) {
    var request = new XMLHttpRequest();
    request.open('GET', path, true);
    request.onload = function () {
        try {
            if (request.status < 200 || request.status >= 300) {
                throw new Error('초기 데이터를 불러오지 못했습니다. 정적 서버에서 페이지를 여세요.');
            }
            callback(JSON.parse(request.responseText));
        } catch (error) {
            showError(error);
        }
    };
    request.onerror = function () {
        notice('초기 데이터를 불러오지 못했습니다. 정적 서버 주소를 확인하세요.', true);
    };
    request.send();
}

function showPage(page) {
    var user = store.profile();
    if (!user && (page === 'meals' || page === 'profile')) {
        page = 'auth';
        showAuthForm('login');
    }
    var sections = ['auth', 'meals', 'foods', 'profile'];
    for (var i = 0; i < sections.length; i++) {
        byId(sections[i] + '-section').hidden = sections[i] !== page;
    }
    var navigation = document.querySelectorAll('.nav-button');
    for (var j = 0; j < navigation.length; j++) {
        var button = navigation[j];
        if (button.getAttribute('data-page') === page) {
            button.classList.add('active');
            button.setAttribute('aria-current', 'page');
        } else {
            button.classList.remove('active');
            button.removeAttribute('aria-current');
        }
    }
    renderAccount(user);
    if (page === 'meals') {
        renderMeals();
    } else if (page === 'foods') {
        searchCatalog();
    } else if (page === 'profile') {
        fillProfile(user);
    }
}

function renderAccount(user) {
    byId('account-name').hidden = !user;
    byId('account-name').textContent = user ? user.name + '님' : '';
    byId('account-button').textContent = user ? '로그아웃' : '로그인';
    byId('profile-nav').hidden = !user;
}

function showAuthForm(mode) {
    var login = mode === 'login';
    byId('auth-title').textContent = login ? '로그인' : '회원가입';
    byId('login-form').hidden = !login;
    byId('register-form').hidden = login;
    byId('show-login').classList.toggle('active', login);
    byId('show-register').classList.toggle('active', !login);
    byId('show-login').setAttribute('aria-pressed', String(login));
    byId('show-register').setAttribute('aria-pressed', String(!login));
}

function bindForm(id, handler) {
    var form = byId(id);
    form.addEventListener('submit', function (event) {
        event.preventDefault();
        notice('');
        try {
            handler(form);
        } catch (error) {
            showError(error);
        }
    });
}

function waitForAccount(form, request, onSuccess) {
    var button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    request.then(function (result) {
        button.disabled = false;
        onSuccess(result);
    }).catch(function (error) {
        button.disabled = false;
        showError(error);
    });
}

function readProfile(form) {
    var parts = form.elements.conditions.value.split(',');
    var conditions = [];
    for (var i = 0; i < parts.length; i++) {
        var condition = parts[i].trim();
        if (condition) {
            conditions.push(condition);
        }
    }
    return {
        name: form.elements.name.value.trim(),
        heightCm: form.elements.heightCm.valueAsNumber,
        weightKg: form.elements.weightKg.valueAsNumber,
        conditions: conditions
    };
}

function fillProfile(profile) {
    var form = byId('profile-form');
    byId('profile-login-id').textContent = '아이디 ' + profile.loginId;
    form.elements.name.value = profile.name;
    form.elements.heightCm.value = profile.heightCm;
    form.elements.weightKg.value = profile.weightKg;
    form.elements.conditions.value = profile.conditions.join(', ');
}

function clearMealPanels() {
    byId('editor-panel').hidden = true;
    byId('detail-panel').hidden = true;
    byId('meal-form').reset();
    byId('food-items').textContent = '';
    byId('detail-note').textContent = '';
}

function prepareMealPage() {
    var meals = store.meals();
    byId('analysis-date').value = meals.length > 0 ? meals[0].date : todayText();
    byId('meal-filter').value = 'all';
    clearMealPanels();
    showPage('meals');
}

function renderAnalysis() {
    var date = byId('analysis-date').value;
    var valid = YamyamDomain.validDate(date);
    var meals = valid ? store.meals(date) : [];
    var analysis = YamyamDomain.analyzeMeals(meals, catalog);
    byId('total-calories').textContent = formatNumber(analysis.total.calories);
    byId('total-carbs').textContent = formatNumber(analysis.total.carbs);
    byId('total-protein').textContent = formatNumber(analysis.total.protein);
    byId('total-fat').textContent = formatNumber(analysis.total.fat);
    byId('analysis-count').textContent = valid
        ? '해당 날짜 식단 ' + meals.length + '건'
        : '분석할 날짜를 선택하세요.';

    var ranking = byId('ranking-list');
    ranking.textContent = '';
    byId('ranking-empty').hidden = analysis.ranking.length > 0;
    for (var i = 0; i < analysis.ranking.length; i++) {
        var entry = analysis.ranking[i];
        var item = document.createElement('li');
        var row = document.createElement('div');
        row.className = 'rank-entry';
        row.appendChild(createTextElement('span', entry.name));
        row.appendChild(createTextElement('strong', formatNumber(entry.calories) + ' kcal'));
        item.appendChild(row);
        ranking.appendChild(item);
    }
}

function appendMealButton(parent, action, id, text) {
    var button = createTextElement('button', text);
    button.type = 'button';
    button.className = 'btn btn-sm btn-outline-secondary';
    if (action === 'delete') {
        button.className = 'btn btn-sm btn-outline-danger';
    }
    button.setAttribute('data-action', action);
    button.setAttribute('data-id', id);
    parent.appendChild(button);
}

function renderMeals() {
    renderAnalysis();
    var meals = [];
    if (byId('meal-filter').value === 'all') {
        meals = store.meals();
    } else if (YamyamDomain.validDate(byId('analysis-date').value)) {
        meals = store.meals(byId('analysis-date').value);
    }
    byId('meal-count').textContent = '식단 ' + meals.length + '건';
    var rows = byId('meal-rows');
    rows.textContent = '';
    if (meals.length === 0) {
        appendEmptyRow(rows, 4, '등록한 식단이 없습니다.');
    }
    for (var i = 0; i < meals.length; i++) {
        var meal = meals[i];
        var row = document.createElement('tr');
        var dateCell = createTextElement('td', meal.date);
        var id = createTextElement('span', 'ID ' + meal.id);
        id.className = 'food-code';
        dateCell.appendChild(id);
        row.appendChild(dateCell);
        row.appendChild(createTextElement('td', meal.type));
        var food = catalog.find(meal.foods[0].foodCode);
        var description = food ? food.name : meal.foods[0].foodCode;
        if (meal.foods.length > 1) {
            description += ' 외 ' + (meal.foods.length - 1) + '개';
        }
        row.appendChild(createTextElement('td', description));
        var cell = document.createElement('td');
        var actions = document.createElement('div');
        actions.className = 'row-actions';
        appendMealButton(actions, 'detail', meal.id, '상세');
        appendMealButton(actions, 'edit', meal.id, '수정');
        appendMealButton(actions, 'delete', meal.id, '삭제');
        cell.appendChild(actions);
        row.appendChild(cell);
        rows.appendChild(row);
    }
}

function appendEmptyRow(rows, columns, message) {
    var row = document.createElement('tr');
    var cell = createTextElement('td', message);
    cell.colSpan = columns;
    cell.className = 'muted';
    row.appendChild(cell);
    rows.appendChild(row);
}

function showDetail(meal) {
    byId('detail-meta').textContent = 'ID ' + meal.id + ' · ' + meal.date + ' · ' + meal.type;
    var list = byId('detail-foods');
    list.textContent = '';
    for (var i = 0; i < meal.foods.length; i++) {
        var item = meal.foods[i];
        var food = catalog.find(item.foodCode);
        list.appendChild(createTextElement('li', food.name + ' · ' + item.amount + ' ' + item.unit));
    }
    var total = YamyamDomain.analyzeMeals([meal], catalog).total;
    byId('detail-total').textContent = formatNumber(total.calories) + ' kcal · 탄수화물 '
        + formatNumber(total.carbs) + ' g · 단백질 ' + formatNumber(total.protein)
        + ' g · 지방 ' + formatNumber(total.fat) + ' g';
    byId('detail-note').textContent = meal.note || '';
    byId('editor-panel').hidden = true;
    byId('detail-panel').hidden = false;
    byId('detail-title').focus();
}

function updateFoodOptions(query, select, code) {
    var matches = catalog.search(query, false);
    select.textContent = '';
    var placeholder = createTextElement('option', '음식을 선택하세요');
    placeholder.value = '';
    select.appendChild(placeholder);
    for (var i = 0; i < Math.min(matches.length, 20); i++) {
        var food = matches[i];
        var option = createTextElement('option', food.name + ' · ' + food.foodCode);
        option.value = food.foodCode;
        select.appendChild(option);
        if (food.foodCode === code || (matches.length === 1 && query.trim())) {
            select.value = food.foodCode;
        }
    }
}

function addFoodRow(item) {
    var list = byId('food-items');
    if (list.children.length >= 30) {
        notice('음식은 최대 30개까지 입력할 수 있습니다.', true);
        return;
    }
    var row = byId('food-row-template').content.firstElementChild.cloneNode(true);
    var query = row.querySelector('.item-query');
    var select = row.querySelector('.item-food');
    var amount = row.querySelector('.item-amount');
    var unit = row.querySelector('.item-unit');

    function showUnit() {
        var food = catalog.find(select.value);
        unit.textContent = food ? '(' + food.basisUnit + ')' : '';
    }

    if (item) {
        query.value = item.foodCode;
        amount.value = item.amount;
    }
    updateFoodOptions(query.value, select, item ? item.foodCode : '');
    showUnit();

    query.addEventListener('input', function () {
        updateFoodOptions(query.value, select, select.value);
        showUnit();
    });
    select.addEventListener('change', showUnit);
    row.querySelector('.item-remove').addEventListener('click', function () {
        if (list.children.length > 1) {
            list.removeChild(row);
        } else {
            notice('음식이 한 개 이상 필요합니다.', true);
        }
    });
    list.appendChild(row);
    return query;
}

function openEditor(meal) {
    byId('detail-panel').hidden = true;
    byId('editor-panel').hidden = false;
    var form = byId('meal-form');
    form.reset();
    form.elements.mealId.value = '';
    byId('food-items').textContent = '';
    if (meal) {
        byId('editor-title').textContent = '식단 수정';
        form.elements.mealId.value = meal.id;
        form.elements.date.value = meal.date;
        form.elements.type.value = meal.type;
        form.elements.note.value = meal.note || '';
        for (var i = 0; i < meal.foods.length; i++) {
            addFoodRow(meal.foods[i]);
        }
    } else {
        byId('editor-title').textContent = '식단 작성';
        form.elements.date.value = byId('analysis-date').value || todayText();
        addFoodRow();
    }
    form.elements.date.focus();
}

function saveMealForm(form) {
    var rows = byId('food-items').children;
    var foods = [];
    for (var i = 0; i < rows.length; i++) {
        var code = rows[i].querySelector('.item-food').value;
        var food = catalog.find(code);
        if (!food) {
            throw new Error('음식을 목록에서 선택하세요.');
        }
        foods.push({
            foodCode: code,
            amount: rows[i].querySelector('.item-amount').valueAsNumber,
            unit: food.basisUnit
        });
    }
    var meal = store.saveMeal({
        id: Number(form.elements.mealId.value),
        date: form.elements.date.value,
        type: form.elements.type.value,
        note: form.elements.note.value.trim(),
        foods: foods
    });
    byId('analysis-date').value = meal.date;
    clearMealPanels();
    renderMeals();
    notice('식단을 저장했습니다.');
}

function searchCatalog() {
    var prefixOnly = byId('food-search-mode').value === 'prefix';
    foodMatches = catalog.search(byId('food-query').value, prefixOnly);
    foodPage = 0;
    renderCatalog();
}

function renderCatalog() {
    var rows = byId('catalog-rows');
    rows.textContent = '';
    if (foodMatches.length === 0) {
        appendEmptyRow(rows, 6, '검색 결과가 없습니다.');
    }
    var start = foodPage * foodPageSize;
    var end = Math.min(start + foodPageSize, foodMatches.length);
    for (var i = start; i < end; i++) {
        var food = foodMatches[i];
        var row = document.createElement('tr');
        var name = createTextElement('td', food.name);
        var code = createTextElement('span', food.foodCode);
        code.className = 'food-code';
        name.appendChild(code);
        row.appendChild(name);
        row.appendChild(createTextElement('td', food.basisAmount + ' ' + food.basisUnit));
        row.appendChild(createTextElement('td', formatNumber(food.calories) + ' kcal'));
        row.appendChild(createTextElement('td', formatNumber(food.carbs) + ' g'));
        row.appendChild(createTextElement('td', formatNumber(food.protein) + ' g'));
        row.appendChild(createTextElement('td', formatNumber(food.fat) + ' g'));
        rows.appendChild(row);
    }
    var pageCount = Math.max(1, Math.ceil(foodMatches.length / foodPageSize));
    byId('food-count').textContent = '음식 ' + foodMatches.length.toLocaleString('ko-KR') + '종';
    byId('food-page').textContent = (foodPage + 1) + ' / ' + pageCount;
    byId('food-prev').disabled = foodPage <= 0;
    byId('food-next').disabled = foodPage >= pageCount - 1;
}

function bindNavigation() {
    var pages = document.querySelectorAll('[data-page]');
    for (var i = 0; i < pages.length; i++) {
        pages[i].addEventListener('click', function () {
            notice('');
            showPage(this.getAttribute('data-page'));
        });
    }
    byId('account-button').addEventListener('click', function () {
        notice('');
        if (store.profile()) {
            store.logout();
            clearMealPanels();
            showPage('meals');
            notice('로그아웃했습니다.');
        } else {
            showAuthForm('login');
            showPage('auth');
        }
    });
}

function bindAccountActions() {
    byId('show-login').addEventListener('click', function () {
        notice('');
        showAuthForm('login');
    });
    byId('show-register').addEventListener('click', function () {
        notice('');
        showAuthForm('register');
    });
    byId('fill-demo').addEventListener('click', function () {
        notice('');
        showAuthForm('login');
        var form = byId('login-form');
        form.elements.loginId.value = 'demo01';
        form.elements.password.value = 'DemoMeal01!';
        form.querySelector('button[type="submit"]').focus();
    });
    bindForm('login-form', function (form) {
        var request = store.login(form.elements.loginId.value.trim(), form.elements.password.value);
        waitForAccount(form, request, function () {
            form.reset();
            prepareMealPage();
        });
    });
    bindForm('register-form', function (form) {
        var request = store.register(form.elements.loginId.value.trim(), form.elements.password.value,
            readProfile(form));
        waitForAccount(form, request, function (user) {
            form.reset();
            showAuthForm('login');
            byId('login-id').value = user.loginId;
            byId('login-password').focus();
            notice('가입했습니다. 로그인하세요.');
        });
    });
    bindForm('profile-form', function (form) {
        var profile = store.updateProfile(readProfile(form));
        renderAccount(profile);
        fillProfile(profile);
        notice('프로필을 저장했습니다.');
    });
    bindForm('delete-form', function (form) {
        if (!window.confirm('이 계정과 식단 기록을 삭제하시겠습니까?')) {
            return;
        }
        waitForAccount(form, store.deleteAccount(form.elements.password.value), function () {
            form.reset();
            clearMealPanels();
            showPage('meals');
            notice('회원 탈퇴를 완료했습니다.');
        });
    });
}

function bindMealActions() {
    byId('analysis-date').addEventListener('change', function () {
        notice('');
        try {
            renderMeals();
        } catch (error) {
            showError(error);
        }
    });
    byId('meal-filter').addEventListener('change', renderMeals);
    byId('new-meal').addEventListener('click', function () {
        notice('');
        openEditor(null);
    });
    byId('close-editor').addEventListener('click', function () {
        byId('editor-panel').hidden = true;
    });
    byId('close-detail').addEventListener('click', function () {
        byId('detail-panel').hidden = true;
    });
    byId('add-food-row').addEventListener('click', function () {
        var query = addFoodRow();
        if (query) {
            query.focus();
        }
    });
    byId('meal-rows').addEventListener('click', function (event) {
        var button = event.target;
        var action = button.getAttribute('data-action');
        if (!action) {
            return;
        }
        var id = Number(button.getAttribute('data-id'));
        notice('');
        try {
            if (action === 'detail') {
                showDetail(store.meal(id));
            } else if (action === 'edit') {
                openEditor(store.meal(id));
            } else if (action === 'delete' && window.confirm('이 식단을 삭제하시겠습니까?')) {
                store.deleteMeal(id);
                clearMealPanels();
                renderMeals();
                notice('식단을 삭제했습니다.');
            }
        } catch (error) {
            showError(error);
        }
    });
    bindForm('meal-form', saveMealForm);
}

function bindFoodActions() {
    bindForm('food-search-form', searchCatalog);
    byId('food-prev').addEventListener('click', function () {
        foodPage--;
        renderCatalog();
    });
    byId('food-next').addEventListener('click', function () {
        foodPage++;
        renderCatalog();
    });
}

loadJson('data/foods.json', function (foodData) {
    catalog = YamyamDomain.createFoodCatalog(foodData.foods);
    loadJson('data/users.json', function (userData) {
        loadJson('data/diet-data.json', function (mealData) {
            store = createStore(window.localStorage, window.sessionStorage, catalog, window.crypto);
            store.initialize(userData.users, mealData.meals);
            bindNavigation();
            bindAccountActions();
            bindMealActions();
            bindFoodActions();
            notice('');
            if (store.profile()) {
                prepareMealPage();
            } else {
                showPage('meals');
            }
        });
    });
});
