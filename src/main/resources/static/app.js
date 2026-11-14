(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const state = {
    menu: null, cart: [], orders: [], category: 'ALL', query: '',
    editing: null, dish: null, opener: null, placing: false,
    quote: null, quoteSequence: 0, dialogSequence: 0, quoteTimer: null,
    dialogTimer: null, requestId: null, fingerprint: null, linePrices: new Map()
  };
  const cartKey = 'hearth-table-cart-v1';
  const moneyFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2, useGrouping: true });
  const money = value => `${moneyFormat.format(Number(value))} ₸`;
  const categories = { PIZZA: 'Пицца', PASTA: 'Паста', DRINK: 'Напитки' };
  const statusLabels = { PLACED: 'Получен', PREPARING: 'Готовится', READY: 'Готов', COMPLETED: 'Завершён' };
  // Only presentation is translated; IDs, prices and request payloads stay server-owned.
  const menuCopy = {
    margherita: { name: 'Маргарита', description: 'Томаты Сан-Марцано, моцарелла, базилик' },
    pepperoni: { name: 'Пепперони', description: 'Томаты, моцарелла, пепперони, орегано' },
    garden: { name: 'Овощная', description: 'Сладкий перец, томаты черри, цукини, моцарелла' },
    alfredo: { name: 'Альфредо с курицей', description: 'Фетучини, курица, сливочный соус с пармезаном' },
    pesto: { name: 'Паста с песто', description: 'Пенне, песто из базилика, пармезан, томаты черри' },
    lemonade: { name: 'Домашний лимонад', description: 'Лимон, мята, газированная вода' }
  };
  const toppingLabels = { cheese: 'Доп. сыр', mushrooms: 'Грибы', olives: 'Оливки' };
  const discountLabels = { NONE: 'Без скидки', STUDENT: 'Студентам', PICKUP: 'С собой' };
  const dishName = dish => menuCopy[dish.id]?.name || dish.name;
  const dishDescription = dish => menuCopy[dish.id]?.description || dish.description;
  const dishPlural = new Intl.PluralRules('ru-RU');
  const dishForms = { one: 'блюдо', few: 'блюда', many: 'блюд', other: 'блюда' };
  const eventMessages = {
    CUSTOMER: { PLACED: 'Заказ получен.', PREPARING: 'Заказ готовится.', READY: 'Заказ готов.', COMPLETED: 'Заказ завершён.' },
    KITCHEN: { PLACED: 'Новый заказ.', PREPARING: 'Приготовление начато.', READY: 'Готов к выдаче.', COMPLETED: 'Заказ закрыт.' }
  };
  const errorMessages = {
    'PICKUP discount is only available for takeaway orders': 'Скидка «С собой» доступна только для заказов навынос.',
    'Choose a valid discount': 'Выберите доступную скидку.',
    'Choose up to three valid toppings': 'Можно выбрать до 3 добавок.',
    'A topping can only be added once per item': 'Каждую добавку можно выбрать только один раз.',
    'Toppings are available for pizza and pasta only': 'Добавки доступны только для пиццы и пасты.',
    'Quantity must be between 1 and 10': 'Выберите количество от 1 до 10.',
    'Customer name must contain 2 to 60 characters': 'Имя должно содержать от 2 до 60 символов.',
    'Choose dine-in or takeaway': 'Выберите: в зале или с собой.',
    'Dine-in orders need a table number between 1 and 30': 'Выберите стол от 1 до 30.',
    'Takeaway orders do not have a table number': 'Для заказа с собой номер стола не нужен.',
    'Choose between 1 and 20 menu items': 'В заказе должно быть от 1 до 20 позиций.',
    'An order must have 1 to 20 lines': 'В заказе должно быть от 1 до 20 позиций.',
    'Note must not exceed 300 characters': 'Комментарий — не более 300 символов.',
    'This order is already completed': 'Этот заказ уже завершён.',
    'Order statuses must advance one step at a time': 'Статус можно изменить только на следующий.',
    'Demo limit reached: 20 orders per session': 'Лимит демо: 20 заказов за сеанс.',
    'Order not found': 'Заказ не найден.'
  };

  function orderItemName(name) {
    return name.split(' + ').map(part => {
      const dish = state.menu.items.find(item => item.name === part);
      if (dish) return dishName(dish);
      const topping = state.menu.toppings.find(item => item.name === part);
      return topping ? toppingLabels[topping.id] || part : part;
    }).join(' + ');
  }
  const imagePath = id => `/images/${encodeURIComponent(id)}.jpg`;
  const el = (tag, className, text) => {
    const element = document.createElement(tag);
    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;
    return element;
  };

  async function api(path, options = {}) {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch(path, { credentials: 'same-origin', ...options, signal: controller.signal,
        headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers } });
      const body = await response.json().catch(() => null);
      if (!response.ok) throw new Error(errorMessages[body?.message] || body?.message || `Ошибка ${response.status}. Попробуйте ещё раз.`);
      if (body === null) throw new Error('Не удалось прочитать ответ сервера. Попробуйте ещё раз.');
      return body;
    } catch (error) {
      if (error.name === 'AbortError') throw new Error('Сервер не ответил. Попробуйте ещё раз.');
      if (error instanceof TypeError) throw new Error('Нет соединения. Проверьте интернет и попробуйте ещё раз.');
      throw error;
    } finally { clearTimeout(timeout); }
  }
  const post = (path, body) => api(path, { method: 'POST', body: JSON.stringify(body) });

  function errorAt(selector, message = '') {
    const box = $(selector);
    if (!box) return;
    box.replaceChildren();
    box.hidden = !message;
    if (message) box.append(el('span', '', message));
  }

  let toastTimer;
  function toast(message) {
    const box = $('#toast');
    clearTimeout(toastTimer);
    box.textContent = message;
    box.hidden = false;
    toastTimer = window.setTimeout(() => { box.hidden = true; }, 4000);
  }

  function findDish(id) { return state.menu.items.find(item => item.id === id); }
  function fulfillment() { return $('input[name="fulfillment"]:checked')?.value || 'DINE_IN'; }
  function toppingNames(ids) {
    return ids.map(id => toppingLabels[id] || state.menu.toppings.find(topping => topping.id === id)?.name).filter(Boolean);
  }
  function persistCart() {
    try { localStorage.setItem(cartKey, JSON.stringify(state.cart)); } catch (_) { /* A private browser can disable persistence. */ }
  }
  function restoreCart() {
    try {
      const saved = JSON.parse(localStorage.getItem(cartKey) || '[]');
      if (!Array.isArray(saved)) return;
      const knownToppings = new Set(state.menu.toppings.map(topping => topping.id));
      state.cart = saved.slice(0, 20).filter(line => line && findDish(line.menuItemId)
        && Number.isInteger(line.quantity) && line.quantity >= 1 && line.quantity <= 10
        && Array.isArray(line.toppings) && line.toppings.length <= 3
        && new Set(line.toppings).size === line.toppings.length
        && line.toppings.every(id => knownToppings.has(id))
        && (findDish(line.menuItemId).category !== 'DRINK' || !line.toppings.length))
        .map(line => ({ menuItemId: line.menuItemId, quantity: line.quantity, toppings: [...line.toppings] }));
    } catch (_) { state.cart = []; }
  }

  function renderMenu() {
    const grid = $('#menu-grid');
    const items = state.menu.items.filter(item => (state.category === 'ALL' || item.category === state.category)
      && `${item.name} ${item.description} ${dishName(item)} ${dishDescription(item)}`.toLowerCase().includes(state.query.toLowerCase().trim()));
    grid.replaceChildren();
    $('#menu-count').textContent = `${items.length} ${dishForms[dishPlural.select(items.length)]}`;
    if (!items.length) {
      grid.append(el('p', 'menu-empty', 'Ничего не найдено. Измените запрос.'));
      return;
    }
    for (const item of items) {
      const card = el('article', 'dish-card');
      card.dataset.category = item.category;
      const imageWrap = el('div', 'dish-image-wrap');
      const imageButton = el('button', 'dish-image-button');
      imageButton.type = 'button'; imageButton.setAttribute('aria-label', `Открыть блюдо «${dishName(item)}»`);
      imageButton.addEventListener('click', () => openDish(item, null, imageButton));
      const image = el('img', 'dish-image');
      image.src = imagePath(item.id); image.alt = ''; image.loading = 'lazy'; image.decoding = 'async';
      imageButton.append(image); imageWrap.append(imageButton);
      const body = el('div', 'dish-body');
      const title = el('h3', 'dish-title', dishName(item));
      const description = el('p', 'dish-description', dishDescription(item));
      const bottom = el('div', 'dish-bottom');
      const price = el('strong', 'dish-price', money(item.price));
      const add = el('button', 'dish-add', 'В корзину');
      add.type = 'button'; add.setAttribute('aria-label', `В корзину: ${dishName(item)}`);
      add.addEventListener('click', () => openDish(item, null, add));
      bottom.append(price, add); body.append(title, description, bottom); card.append(imageWrap, body); grid.append(card);
    }
  }

  function renderCart() {
    const container = $('#cart-items'); container.replaceChildren();
    const quantity = state.cart.reduce((count, line) => count + line.quantity, 0);
    $('#cart-count').textContent = quantity;
    $('#cart-empty').hidden = quantity > 0;
    $('#checkout-form').hidden = quantity === 0;
    for (const [index, line] of state.cart.entries()) {
      const dish = findDish(line.menuItemId);
      const row = el('div', 'cart-row');
      const image = el('img', 'cart-thumb'); image.src = imagePath(dish.id); image.alt = '';
      const body = el('div', 'cart-row-body');
      const top = el('div', 'cart-row-top');
      const title = el('button', 'cart-edit cart-row-title', dishName(dish));
      title.type = 'button'; title.setAttribute('aria-label', `Изменить: ${dishName(dish)}`);
      title.disabled = state.placing;
      title.addEventListener('click', () => openDish(dish, index, title));
      const remove = el('button', 'cart-remove', '×');
      remove.type = 'button'; remove.setAttribute('aria-label', `Удалить: ${dishName(dish)}`); remove.disabled = state.placing;
      remove.addEventListener('click', () => { if (!state.placing) { state.cart.splice(index, 1); cartChanged(); } });
      top.append(title, remove);
      const options = el('p', 'cart-row-options', toppingNames(line.toppings).join(', '));
      options.hidden = !line.toppings.length;
      const bottom = el('div', 'cart-row-bottom');
      const stepper = el('div', 'quantity-stepper');
      const minus = el('button', 'step-minus', '−'); minus.type = 'button'; minus.disabled = state.placing || line.quantity <= 1;
      minus.setAttribute('aria-label', `Уменьшить количество: ${dishName(dish)}`);
      const plus = el('button', 'step-plus', '+'); plus.type = 'button'; plus.disabled = state.placing || line.quantity >= 10;
      plus.setAttribute('aria-label', `Увеличить количество: ${dishName(dish)}`);
      minus.addEventListener('click', () => { if (!state.placing && line.quantity > 1) { line.quantity--; cartChanged(); } });
      plus.addEventListener('click', () => { if (!state.placing && line.quantity < 10) { line.quantity++; cartChanged(); } });
      stepper.append(minus, el('span', 'quantity-value', line.quantity), plus);
      const price = el('strong', 'cart-row-price', '…');
      const key = JSON.stringify(line);
      if (!state.linePrices.has(key)) {
        state.linePrices.set(key, post('/api/orders/quote', { items: [line], fulfillment: 'TAKEAWAY', discountCode: 'NONE' })
          .catch(error => { state.linePrices.delete(key); throw error; }));
      }
      state.linePrices.get(key).then(quote => { price.textContent = money(quote.subtotal); })
        .catch(() => { price.textContent = '—'; });
      bottom.append(stepper, price); body.append(top, options, bottom); row.append(image, body); container.append(row);
    }
  }

  function cartChanged() {
    persistCart(); renderCart(); queueQuote();
  }

  function quotePayload() {
    return { items: state.cart.map(line => ({ ...line, toppings: [...line.toppings] })),
      fulfillment: fulfillment(), discountCode: $('#discount-code').value || 'NONE' };
  }

  function queueQuote() {
    if (!state.menu) return;
    clearTimeout(state.quoteTimer);
    const sequence = ++state.quoteSequence;
    state.quote = null;
    $('#place-order').disabled = true;
    errorAt('#checkout-error');
    for (const id of ['#quote-subtotal', '#quote-discount', '#quote-total']) $(id).textContent = state.cart.length ? '…' : money(0);
    for (const id of ['#quote-subtotal', '#quote-discount']) $(id).parentElement.hidden = true;
    if (!state.cart.length) return;
    state.quoteTimer = window.setTimeout(async () => {
      try {
        const quote = await post('/api/orders/quote', quotePayload());
        if (sequence !== state.quoteSequence) return;
        state.quote = quote;
        $('#quote-subtotal').textContent = money(quote.subtotal);
        $('#quote-discount').textContent = Number(quote.discount) ? `−${money(quote.discount)}` : money(0);
        $('#quote-total').textContent = money(quote.total);
        for (const id of ['#quote-subtotal', '#quote-discount']) $(id).parentElement.hidden = !(Number(quote.discount) > 0);
        $('#place-order').disabled = state.placing;
      } catch (error) {
        if (sequence !== state.quoteSequence) return;
        $('#quote-total').textContent = 'Недоступно';
        errorAt('#checkout-error', error.message);
        const retry = el('button', 'retry-button', 'Повторить'); retry.type = 'button';
        retry.addEventListener('click', queueQuote); $('#checkout-error').append(retry);
      }
    }, 220);
  }

  function openDish(dish, editing = null, opener = null) {
    if (state.placing) return;
    state.dish = dish; state.editing = editing; state.opener = opener;
    const line = editing === null ? null : state.cart[editing];
    $('#dialog-name').textContent = dishName(dish);
    $('#dialog-description').textContent = dishDescription(dish);
    $('#dialog-price').textContent = money(dish.price);
    $('#dialog-category').textContent = categories[dish.category] || dish.category;
    $('#dialog-image').src = imagePath(dish.id); $('#dialog-image').alt = dishName(dish);
    $('#dialog-quantity').value = line?.quantity || 1;
    $('#add-to-cart span').textContent = editing === null ? 'В корзину' : 'Сохранить';
    const choices = $('#topping-options'); choices.replaceChildren();
    choices.closest('.toppings-field').hidden = dish.category === 'DRINK';
    if (dish.category !== 'DRINK') {
      for (const topping of state.menu.toppings) {
        const label = el('label', 'topping-option');
        const input = el('input'); input.type = 'checkbox'; input.name = 'toppings'; input.value = topping.id;
        input.checked = Boolean(line?.toppings.includes(topping.id));
        input.addEventListener('change', queueDialogQuote);
        label.append(input, el('span', 'topping-name', toppingLabels[topping.id] || topping.name), el('span', 'topping-price', `+${money(topping.price)}`));
        choices.append(label);
      }
    }
    errorAt('#dialog-error');
    $('#customize-dialog').showModal();
    document.body.classList.add('dialog-open');
    queueDialogQuote();
  }

  function dialogLine() {
    return { menuItemId: state.dish.id, quantity: Number($('#dialog-quantity').value),
      toppings: $$('#topping-options input:checked').map(input => input.value).sort() };
  }

  function queueDialogQuote() {
    clearTimeout(state.dialogTimer);
    const sequence = ++state.dialogSequence;
    const line = dialogLine();
    $('#dialog-minus').disabled = line.quantity <= 1;
    $('#dialog-plus').disabled = line.quantity >= 10;
    $('#add-to-cart').disabled = true; $('#dialog-total').textContent = '…';
    errorAt('#dialog-error');
    if (!Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > 10) {
      errorAt('#dialog-error', 'Выберите количество от 1 до 10.'); return;
    }
    if (line.toppings.length > 3) {
      errorAt('#dialog-error', 'Можно выбрать до 3 добавок.'); return;
    }
    state.dialogTimer = window.setTimeout(async () => {
      try {
        const quote = await post('/api/orders/quote', { items: [line], fulfillment: 'TAKEAWAY', discountCode: 'NONE' });
        if (sequence !== state.dialogSequence) return;
        $('#dialog-total').textContent = money(quote.total);
        $('#add-to-cart').disabled = false;
      } catch (error) {
        if (sequence !== state.dialogSequence) return;
        $('#dialog-total').textContent = 'Недоступно'; errorAt('#dialog-error', error.message);
        const retry = el('button', 'retry-button', 'Повторить'); retry.type = 'button'; retry.addEventListener('click', queueDialogQuote);
        $('#dialog-error').append(retry);
      }
    }, 120);
  }

  function closeDialog() { $('#customize-dialog').close(); }
  function addToCart(event) {
    event.preventDefault();
    if ($('#add-to-cart').disabled || !$('#customize-form').reportValidity() || state.placing) return;
    const line = dialogLine();
    if (state.editing === null) {
      if (state.cart.length >= 20) { errorAt('#dialog-error', 'В корзине может быть до 20 позиций.'); return; }
      state.cart.push(line);
    } else { state.cart[state.editing] = line; }
    const message = state.editing === null ? `${dishName(state.dish)} — в корзине` : 'Корзина обновлена';
    closeDialog(); cartChanged(); toast(message);
  }

  function updateFulfillment() {
    const dineIn = fulfillment() === 'DINE_IN';
    $('#table-field').hidden = !dineIn;
    $('#table-number').required = dineIn;
    $('#table-number').disabled = !dineIn || state.placing;
    queueQuote();
  }

  function setCheckoutBusy(busy) {
    state.placing = busy;
    $$('#checkout-form input, #checkout-form select, #checkout-form textarea, #checkout-form button').forEach(input => { input.disabled = busy; });
    $('#table-number').disabled = busy || fulfillment() === 'TAKEAWAY';
    $('#place-order').disabled = busy || !state.quote;
    $('#place-order').setAttribute('aria-busy', String(busy));
    $('#place-order').textContent = busy ? 'Оформляем…' : 'Оформить заказ';
    renderCart();
  }

  async function placeOrder(event) {
    event.preventDefault();
    if (state.placing || !state.quote || !state.cart.length || !$('#checkout-form').reportValidity()) return;
    const customerName = $('#customer-name').value.trim();
    if (customerName.length < 2) { errorAt('#checkout-error', 'Введите имя: минимум 2 символа.'); $('#customer-name').focus(); return; }
    const request = { ...quotePayload(), customerName, tableNumber: fulfillment() === 'DINE_IN' ? Number($('#table-number').value) : null,
      note: $('#order-note').value.trim() };
    const fingerprint = JSON.stringify(request);
    if (state.fingerprint !== fingerprint || !state.requestId) {
      state.requestId = crypto.randomUUID(); state.fingerprint = fingerprint;
    }
    request.requestId = state.requestId;
    setCheckoutBusy(true); errorAt('#checkout-error');
    try {
      const order = await post('/api/orders', request);
      state.orders = [order, ...state.orders.filter(existing => existing.id !== order.id)];
      state.cart = []; state.requestId = null; state.fingerprint = null; state.quote = null;
      persistCart(); renderOrders();
      window.location.hash = 'orders';
      toast('Заказ оформлен');
    } catch (error) { errorAt('#checkout-error', error.message); }
    finally { setCheckoutBusy(false); }
  }

  const dateLabel = value => new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit', day: 'numeric', month: 'short' }).format(new Date(value));
  function renderOrders() {
    const container = $('#orders-list');
    const expanded = new Map($$('.order-card', container).map(card => [card.dataset.orderId, {
      details: $('.order-details', card)?.open,
      demo: $('.demo-tools', card)?.open
    }]));
    container.replaceChildren();
    $('#order-count').textContent = state.orders.length;
    $('#order-count').hidden = state.orders.length === 0;
    if (!state.orders.length) {
      const empty = el('div', 'empty-orders');
      empty.append(el('h2', '', 'Заказов пока нет'));
      const link = el('a', 'empty-orders-link', 'В меню'); link.href = '#menu-list'; empty.append(link); container.append(empty);
      return;
    }
    const orders = [...state.orders].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    for (const order of orders) {
      const card = el('article', 'order-card');
      card.dataset.orderId = order.id;
      const top = el('div', 'order-card-top');
      const title = el('div');
      title.append(el('h2', 'order-ref', `#${order.id.slice(0, 8).toUpperCase()}`),
        el('p', 'order-meta', `${dateLabel(order.createdAt)} · ${order.fulfillment === 'DINE_IN' ? `Стол ${order.tableNumber}` : 'С собой'}`));
      top.append(title, el('span', 'order-status', statusLabels[order.status] || order.status)); card.append(top);
      const statuses = state.menu.statuses || [];
      const current = statuses.indexOf(order.status);
      const lines = el('div', 'order-lines');
      for (const item of order.items) {
        const line = el('div', 'order-line'); line.append(el('span', '', `${item.quantity} × ${orderItemName(item.name)}`), el('strong', '', money(item.lineTotal))); lines.append(line);
      }
      if (Number(order.discount)) {
        const line = el('div', 'order-line order-discount'); line.append(el('span', '', 'Скидка'), el('strong', '', `−${money(order.discount)}`)); lines.append(line);
      }
      const total = el('div', 'order-summary');
      const totalRow = el('div'); totalRow.append(el('span', '', 'Итого'), el('strong', '', money(order.total)));
      total.append(totalRow); lines.append(total);
      card.append(lines);

      function receiptLink(type, format, label) {
        const link = el('a', 'receipt-link', label);
        link.href = `/api/orders/${encodeURIComponent(order.id)}/receipt?type=${type}&format=${format}`;
        link.target = '_blank'; link.rel = 'noopener noreferrer';
        return link;
      }
      function eventFeed(audience) {
        const events = el('ul', 'event-feed');
        for (const event of [...order.events].reverse().filter(event => event.audience === audience)) {
          const item = el('li', 'event-item');
          const time = el('time', '', dateLabel(event.occurredAt)); time.dateTime = event.occurredAt;
          item.append(time, el('p', '', eventMessages[audience]?.[event.status] || event.message)); events.append(item);
        }
        return events;
      }

      const actions = el('div', 'order-actions');
      actions.append(receiptLink('customer', 'html', 'Чек')); card.append(actions);

      const details = el('details', 'order-details');
      details.open = Boolean(expanded.get(order.id)?.details);
      details.append(el('summary', '', 'Подробнее'));
      const extra = el('div', 'order-extra');
      extra.append(el('p', 'order-customer', `Имя: ${order.customerName}`));
      if (order.note) extra.append(el('p', 'order-note', `Комментарий: ${order.note}`));
      extra.append(el('h3', 'order-section-title', 'История'), eventFeed('CUSTOMER'));

      const demo = el('details', 'demo-tools');
      demo.open = Boolean(expanded.get(order.id)?.demo);
      demo.append(el('summary', '', 'Демо-режим'));
      const tools = el('div', 'demo-tools-body');
      const receipts = el('div', 'receipt-links');
      receipts.append(receiptLink('customer', 'text', 'Чек — текст'),
        receiptLink('kitchen', 'html', 'Заказ для кухни'), receiptLink('kitchen', 'text', 'Заказ для кухни — текст'));
      tools.append(receipts, el('h3', 'order-section-title', 'Кухня'), eventFeed('KITCHEN'));
      if (current >= 0 && current < statuses.length - 1) {
        const advance = el('button', 'advance-order', 'Следующий статус (демо)'); advance.type = 'button';
        advance.addEventListener('click', async () => {
          advance.disabled = true; errorAt('#orders-error');
          try {
            const updated = await post(`/api/orders/${encodeURIComponent(order.id)}/advance`, {});
            state.orders = state.orders.map(item => item.id === updated.id ? updated : item);
            renderOrders(); toast(statusLabels[updated.status] || updated.status);
            const updatedCard = $$('.order-card', container).find(card => card.dataset.orderId === order.id);
            if (updatedCard) ($('.advance-order', updatedCard) || $('.demo-tools > summary', updatedCard))?.focus();
          } catch (error) { errorAt('#orders-error', error.message); advance.disabled = false; }
        });
        tools.append(advance);
      }
      demo.append(tools); extra.append(demo); details.append(extra); card.append(details); container.append(card);
    }
  }

  async function loadOrders() {
    if (!state.menu) return;
    const refresh = $('#refresh-orders');
    const restoreFocus = document.activeElement === refresh;
    refresh.disabled = true; errorAt('#orders-error');
    try { state.orders = await api('/api/orders'); renderOrders(); }
    catch (error) { errorAt('#orders-error', error.message); }
    finally {
      refresh.disabled = false;
      if (restoreFocus && document.activeElement === document.body) refresh.focus();
    }
  }

  function route() {
    const orders = window.location.hash === '#orders';
    $('#view-menu').hidden = orders; $('#view-orders').hidden = !orders;
    $$('a[href="#menu"], a[href="#orders"]').forEach(link => {
      const active = orders ? link.hash === '#orders' : link.hash === '#menu';
      link.classList.toggle('active', active);
      if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
    });
    if (orders) { loadOrders(); window.scrollTo({ top: 0, behavior: 'instant' }); }
    else if (window.location.hash === '#menu') window.scrollTo({ top: 0, behavior: 'instant' });
    else if (window.location.hash) requestAnimationFrame(() => document.getElementById(window.location.hash.slice(1))?.scrollIntoView({ behavior: 'smooth' }));
  }

  async function loadMenu() {
    errorAt('#menu-error');
    $('#menu-grid').setAttribute('aria-busy', 'true');
    try {
      state.menu = await api('/api/menu');
      const select = $('#discount-code'); select.replaceChildren();
      for (const option of state.menu.discounts) {
        const percentage = option.name.match(/\d+(?:[.,]\d+)?%/)?.[0];
        const label = discountLabels[option.id] ? `${discountLabels[option.id]}${percentage && option.id !== 'NONE' ? ` −${percentage}` : ''}` : option.name;
        const element = el('option', '', label); element.value = option.id; select.append(element);
      }
      select.value = 'NONE';
      restoreCart(); renderMenu(); renderCart(); queueQuote(); renderOrders();
      if (window.location.hash === '#orders') await loadOrders();
      else loadOrders();
    } catch (error) {
      $('#menu-grid').replaceChildren(); $('#menu-count').textContent = 'Меню недоступно';
      errorAt('#menu-error', error.message);
      const retry = el('button', 'button button-dark', 'Повторить'); retry.type = 'button'; retry.addEventListener('click', loadMenu); $('#menu-error').append(retry);
    } finally { $('#menu-grid').setAttribute('aria-busy', 'false'); }
  }

  $('#menu-search').addEventListener('input', event => { state.query = event.target.value; if (state.menu) renderMenu(); });
  $$('button[data-category]').forEach(button => button.addEventListener('click', () => {
    state.category = button.dataset.category;
    $$('button[data-category]').forEach(item => { const active = item === button; item.classList.toggle('active', active); item.setAttribute('aria-pressed', String(active)); });
    if (state.menu) renderMenu();
  }));
  $$('[data-menu-category]').forEach(link => link.addEventListener('click', () => {
    $$('button[data-category]').find(button => button.dataset.category === link.dataset.menuCategory)?.click();
  }));
  if (!$('#table-number').options.length) {
    const placeholder = el('option', '', 'Выберите стол'); placeholder.value = ''; $('#table-number').append(placeholder);
    for (let table = 1; table <= 30; table++) { const option = el('option', '', `Стол ${table}`); option.value = table; $('#table-number').append(option); }
  }
  $$('input[name="fulfillment"]').forEach(input => input.addEventListener('change', updateFulfillment));
  $('#discount-code').addEventListener('change', queueQuote);
  $('#checkout-form').addEventListener('submit', placeOrder);
  $('#customize-form').addEventListener('submit', addToCart);
  $('#close-dialog').addEventListener('click', closeDialog);
  $('#customize-dialog').addEventListener('close', () => { clearTimeout(state.dialogTimer); state.dialogSequence++; document.body.classList.remove('dialog-open'); state.opener?.focus(); });
  $('#customize-dialog').addEventListener('click', event => {
    if (event.target !== $('#customize-dialog')) return;
    const rect = event.target.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeDialog();
  });
  $('#dialog-quantity').addEventListener('input', queueDialogQuote);
  $('#dialog-minus').addEventListener('click', () => { $('#dialog-quantity').value = Math.max(1, Number($('#dialog-quantity').value) - 1); queueDialogQuote(); });
  $('#dialog-plus').addEventListener('click', () => { $('#dialog-quantity').value = Math.min(10, Number($('#dialog-quantity').value) + 1); queueDialogQuote(); });
  $('#refresh-orders').addEventListener('click', loadOrders);
  window.addEventListener('hashchange', route);
  updateFulfillment(); route(); loadMenu();
})();
