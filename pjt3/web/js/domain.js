'use strict';

function createFoodCatalog(foods) {
    var index = Object.create(null);
    var nutrients = ['calories', 'carbs', 'protein', 'fat'];
    for (var i = 0; i < foods.length; i++) {
        var food = foods[i];
        if (!food.foodCode || !food.name || index[food.foodCode]
            || !Number.isFinite(food.basisAmount) || food.basisAmount <= 0
            || (food.basisUnit !== 'g' && food.basisUnit !== 'mL')) {
            throw new Error('음식 기준량 또는 식품코드를 확인하세요.');
        }
        for (var j = 0; j < nutrients.length; j++) {
            var amount = food[nutrients[j]];
            if (!Number.isFinite(amount) || amount < 0) {
                throw new Error('음식의 영양 성분 값이 올바르지 않습니다.');
            }
        }
        index[food.foodCode] = food;
    }

    function findFood(code) {
        return index[code];
    }

    function searchFoods(keyword, prefixOnly) {
        var query = keyword.trim().toLowerCase();
        var matches = [];
        for (var i = 0; i < foods.length; i++) {
            var food = foods[i];
            var position = food.name.toLowerCase().indexOf(query);
            if (prefixOnly && position === 0) {
                matches.push(food);
            } else if (!prefixOnly && (position !== -1 || food.foodCode.toLowerCase() === query)) {
                matches.push(food);
            }
        }
        return matches;
    }

    return {find: findFood, search: searchFoods};
}

function validDate(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return false;
    }
    var date = new Date(value + 'T00:00:00Z');
    return !isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

function validateMeal(meal, catalog) {
    if (!validDate(meal.date)) {
        throw new Error('날짜를 확인하세요.');
    }
    if (['아침', '점심', '저녁', '간식'].indexOf(meal.type) === -1) {
        throw new Error('식사 구분을 선택하세요.');
    }
    if (typeof meal.note !== 'string' || meal.note.length > 500) {
        throw new Error('메모는 500자 이내로 입력하세요.');
    }
    if (!Array.isArray(meal.foods) || meal.foods.length < 1 || meal.foods.length > 30) {
        throw new Error('음식을 1~30개 선택하세요.');
    }
    for (var i = 0; i < meal.foods.length; i++) {
        var item = meal.foods[i];
        var food = catalog.find(item.foodCode);
        if (!food) {
            throw new Error('음식을 목록에서 선택하세요.');
        }
        if (item.unit !== food.basisUnit) {
            throw new Error(food.name + '의 섭취 단위는 ' + food.basisUnit + '입니다.');
        }
        if (!Number.isFinite(item.amount) || item.amount <= 0 || item.amount > 5000) {
            throw new Error('섭취량은 0보다 크고 5,000 이하여야 합니다.');
        }
    }
}

function validateProfile(profile) {
    if (typeof profile.name !== 'string' || !profile.name.trim() || profile.name.length > 40) {
        throw new Error('이름은 1~40자로 입력하세요.');
    }
    if (!Number.isFinite(profile.heightCm) || profile.heightCm < 50 || profile.heightCm > 250) {
        throw new Error('키는 50~250cm로 입력하세요.');
    }
    if (!Number.isFinite(profile.weightKg) || profile.weightKg < 10 || profile.weightKg > 500) {
        throw new Error('몸무게는 10~500kg으로 입력하세요.');
    }
    if (!Array.isArray(profile.conditions) || profile.conditions.join(', ').length > 300) {
        throw new Error('질환 정보는 300자 이내로 입력하세요.');
    }
}

function analyzeMeals(meals, catalog) {
    var total = {calories: 0, carbs: 0, protein: 0, fat: 0};
    var byCode = Object.create(null);
    var ranking = [];
    for (var i = 0; i < meals.length; i++) {
        for (var j = 0; j < meals[i].foods.length; j++) {
            var item = meals[i].foods[j];
            var food = catalog.find(item.foodCode);
            if (!food) {
                throw new Error('기록에 사용된 음식을 찾을 수 없습니다.');
            }
            var ratio = item.amount / food.basisAmount;
            var calories = food.calories * ratio;
            total.calories += calories;
            total.carbs += food.carbs * ratio;
            total.protein += food.protein * ratio;
            total.fat += food.fat * ratio;
            if (!byCode[food.foodCode]) {
                byCode[food.foodCode] = {foodCode: food.foodCode, name: food.name, calories: 0};
                ranking.push(byCode[food.foodCode]);
            }
            byCode[food.foodCode].calories += calories;
        }
    }
    ranking.sort(function (a, b) {
        if (a.calories !== b.calories) {
            return b.calories - a.calories;
        }
        return a.foodCode.localeCompare(b.foodCode);
    });
    return {total: total, ranking: ranking};
}

var YamyamDomain = {
    createFoodCatalog: createFoodCatalog,
    validDate: validDate,
    validateMeal: validateMeal,
    validateProfile: validateProfile,
    analyzeMeals: analyzeMeals
};

if (typeof module !== 'undefined') {
    module.exports = YamyamDomain;
}
