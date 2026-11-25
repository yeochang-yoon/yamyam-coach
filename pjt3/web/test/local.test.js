'use strict';

var test = require('node:test');
var assert = require('node:assert/strict');
var fs = require('node:fs');
var path = require('node:path');
var crypto = require('node:crypto').webcrypto;
var domain = require('../js/domain.js');
var createStore = require('../js/adapter.js');

function dataFile(name) {
    return JSON.parse(fs.readFileSync(path.join(__dirname, '../data', name), 'utf8'));
}

var foods = dataFile('foods.json').foods;
var users = dataFile('users.json').users;
var meals = dataFile('diet-data.json').meals;
var catalog = domain.createFoodCatalog(foods);
var rice = 'D101-050000000-0001';
var juice = 'D220-716000000-0068';

function memoryStorage() {
    var values = Object.create(null);
    return {
        getItem: function (key) {
            if (key in values) {
                return values[key];
            }
            return null;
        },
        setItem: function (key, value) { values[key] = value; },
        removeItem: function (key) { delete values[key]; }
    };
}

function mealInput(date, amount) {
    return {
        date: date,
        type: '점심',
        note: '',
        foods: [
            { foodCode: rice, amount: amount, unit: 'g' },
            { foodCode: juice, amount: 200, unit: 'mL' }
        ]
    };
}

test('음식코드와 이름을 검색하고 섭취량과 0을 포함해 영양값을 계산한다', function () {
    assert.equal(foods.length, 5789);
    assert.equal(catalog.search(rice.toLowerCase(), false)[0].foodCode, rice);
    assert.ok(catalog.search('현미', true).length > 0);
    assert.ok(catalog.search('미밥', false).length > catalog.search('미밥', true).length);
    assert.equal(catalog.find(juice).basisUnit, 'mL');
    assert.equal(catalog.find(juice).fat, 0);
    var analysis = domain.analyzeMeals([mealInput('2025-04-28', 150)], catalog);
    assert.equal(analysis.total.calories, 358);
    assert.ok(Math.abs(analysis.total.protein - 5.71) < 1e-9);
    assert.equal(analysis.ranking[0].foodCode, rice);
    assert.equal(analysis.ranking[0].calories, 258);
    var doubled = domain.analyzeMeals([mealInput('2025-04-28', 150), mealInput('2025-04-28', 50)], catalog);
    assert.equal(doubled.ranking.length, 2);
    assert.equal(doubled.ranking[0].calories, 344);
    assert.equal(domain.validDate('2025-04-31'), false);
    assert.equal(domain.validDate('2024-02-29'), true);
});

test('데모 로그인 후 식단을 저장, 상세 조회, 수정, 삭제하고 보존한다', function () {
    var storage = memoryStorage();
    var session = memoryStorage();
    var app = createStore(storage, session, catalog, crypto);
    app.initialize(users, meals);
    assert.equal(app.profile(), null);
    assert.throws(function () { app.meals(); }, /로그인/);

    return app.login('demo01', 'DemoMeal01!').then(function (profile) {
        assert.equal(profile.name, '김하늘');
        assert.equal(profile.passwordHash, undefined);
        assert.equal(app.meals().length, 21);
        assert.equal(app.meals('2025-04-21').length, 3);
        assert.equal(app.meal(1).id, 1);
        assert.throws(function () { app.meal(22); }, /찾을 수 없습니다/);
        assert.throws(function () { app.deleteMeal(22); }, /찾을 수 없습니다/);
        var foreign = mealInput('2025-04-28', 150);
        foreign.id = 22;
        assert.throws(function () { app.saveMeal(foreign); }, /찾을 수 없습니다/);

        var invalid = mealInput('2025-04-28', 150);
        invalid.foods[1].unit = 'g';
        assert.throws(function () { app.saveMeal(invalid); }, /섭취 단위/);
        assert.throws(function () { app.saveMeal(mealInput('2025-04-28', 0)); }, /섭취량/);
        var created = app.saveMeal(mealInput('2025-04-28', 150));
        assert.equal(created.userId, 1);

        var loaded = createStore(storage, session, catalog, crypto);
        loaded.initialize(users, meals);
        var original = loaded.meal(created.id);
        assert.equal(original.foods[1].amount, 200);
        original.foods[0].amount = 200;
        original.note = '수정한 기록';
        loaded.saveMeal(original);
        assert.equal(loaded.meal(created.id).note, '수정한 기록');
        assert.equal(domain.analyzeMeals(loaded.meals('2025-04-28'), catalog).total.calories, 444);
        loaded.deleteMeal(created.id);
        assert.equal(loaded.meals('2025-04-28').length, 0);
        loaded.logout();
        assert.equal(loaded.profile(), null);
    });
});

test('회원가입, 중복 검사, 프로필 변경, 비밀번호 확인과 탈퇴가 동작한다', function () {
    var storage = memoryStorage();
    var app = createStore(storage, memoryStorage(), catalog, crypto);
    app.initialize(users, meals);
    var profile = { name: '테스터', heightCm: 170, weightKg: 65, conditions: [] };
    var ownMealId;
    return app.register('tester', 'DemoTest01!', profile).then(function () {
        assert.throws(function () { app.register('tester', 'DemoTest01!', profile); }, /사용 중인/);
        return assert.rejects(app.login('tester', 'wrong'), /비밀번호/);
    }).then(function () {
        return app.login('tester', 'DemoTest01!');
    }).then(function () {
        var updated = { name: '변경', heightCm: 174, weightKg: 67.5, conditions: ['테스트'] };
        app.updateProfile(updated);
        assert.equal(app.profile().name, '변경');
        assert.equal(app.profile().weightKg, 67.5);
        ownMealId = app.saveMeal(mealInput('2025-04-28', 150)).id;
        return assert.rejects(app.deleteAccount('wrong'), /비밀번호/);
    }).then(function () {
        return app.deleteAccount('DemoTest01!');
    }).then(function () {
        assert.equal(app.profile(), null);
        assert.throws(function () { app.login('tester', 'DemoTest01!'); }, /비밀번호/);
        var saved = JSON.parse(storage.getItem('yamyam-pjt3-v2'));
        assert.equal(saved.users.length, 2);
        assert.equal(saved.meals.length, 42);
        assert.ok(saved.meals.every(function (meal) { return meal.id !== ownMealId; }));
        assert.equal(storage.getItem('yamyam-pjt3-v2').indexOf('DemoTest01!'), -1);
    });
});

test('손상된 저장 데이터를 초기화로 덮어쓰지 않는다', function () {
    var storage = memoryStorage();
    storage.setItem('yamyam-pjt3-v2', '{broken');
    var app = createStore(storage, memoryStorage(), catalog, crypto);
    assert.throws(function () { app.initialize(users, meals); }, /읽을 수 없습니다/);
    assert.equal(storage.getItem('yamyam-pjt3-v2'), '{broken');
});
