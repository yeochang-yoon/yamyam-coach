'use strict';

var demoButton = document.getElementById('fill-demo');
if (demoButton) {
    demoButton.addEventListener('click', function () {
        document.getElementById('login-id').value = 'demo01';
        document.getElementById('login-password').value = 'DemoMeal01!';
        document.querySelector('#auth-form button[type="submit"]').focus();
    });
}

var confirmForms = document.querySelectorAll('form[data-confirm]');
for (var i = 0; i < confirmForms.length; i++) {
    confirmForms[i].addEventListener('submit', function (event) {
        if (!window.confirm(this.getAttribute('data-confirm'))) {
            event.preventDefault();
        }
    });
}

var editor = document.getElementById('meal-form');
if (editor) {
    var rows = document.getElementById('food-items');
    var editorMessage = document.getElementById('editor-message');
    var template = document.getElementById('food-row-template');

    function updateUnit(row) {
        var select = row.querySelector('.item-food');
        var option = select.options[select.selectedIndex];
        row.querySelector('.item-unit').textContent = option.getAttribute('data-unit') || '';
    }

    function searchFood(row) {
        var query = row.querySelector('.item-query').value.trim();
        var select = row.querySelector('.item-food');
        var button = row.querySelector('.item-search');
        var message = row.querySelector('.item-message');
        if (!query) {
            message.textContent = '음식명이나 식품코드를 입력하세요.';
            return;
        }

        var request = new XMLHttpRequest();
        request.open('GET', editor.getAttribute('data-food-url') + '?q=' + encodeURIComponent(query), true);
        request.onload = function () {
            button.disabled = false;
            if (request.status !== 200) {
                message.textContent = '음식을 불러오지 못했습니다.';
                return;
            }
            try {
                var foods = JSON.parse(request.responseText);
                select.options.length = 1;
                for (var i = 0; i < foods.length; i++) {
                    var food = foods[i];
                    var option = document.createElement('option');
                    option.value = food.foodCode;
                    option.textContent = food.name + ' · ' + food.foodCode;
                    option.setAttribute('data-unit', food.basisUnit);
                    select.appendChild(option);
                }
                if (foods.length === 1) {
                    select.selectedIndex = 1;
                }
                updateUnit(row);
                message.textContent = foods.length ? '' : '검색 결과가 없습니다.';
            } catch (error) {
                message.textContent = '음식 검색 결과를 읽지 못했습니다.';
            }
        };
        request.onerror = function () {
            button.disabled = false;
            message.textContent = '음식을 불러오지 못했습니다.';
        };
        button.disabled = true;
        request.send();
    }

    function bindRow(row) {
        row.querySelector('.item-search').addEventListener('click', function () {
            searchFood(row);
        });
        row.querySelector('.item-query').addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                if (!row.querySelector('.item-search').disabled) {
                    searchFood(row);
                }
            }
        });
        row.querySelector('.item-food').addEventListener('change', function () {
            updateUnit(row);
        });
        row.querySelector('.item-remove').addEventListener('click', function () {
            if (rows.children.length > 1) {
                rows.removeChild(row);
                editorMessage.hidden = true;
            } else {
                editorMessage.textContent = '음식은 최소 한 개가 필요합니다.';
                editorMessage.hidden = false;
            }
        });
    }

    for (var j = 0; j < rows.children.length; j++) {
        bindRow(rows.children[j]);
    }
    document.getElementById('add-food-row').addEventListener('click', function () {
        if (rows.children.length >= 30) {
            editorMessage.textContent = '음식은 최대 30개까지 입력할 수 있습니다.';
            editorMessage.hidden = false;
            return;
        }
        var row = template.content.firstElementChild.cloneNode(true);
        bindRow(row);
        rows.appendChild(row);
        row.querySelector('.item-query').focus();
    });
}
