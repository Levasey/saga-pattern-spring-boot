(function () {
  const STORAGE_KEY = 'sagaDemoProductsApiBase';
  const DEFAULT_PRODUCTS_BASE = 'http://localhost:8081';

  const $ = (sel) => document.querySelector(sel);

  const productsBaseInput = $('#productsBase');
  const productRows = $('#productRows');
  const orderProductId = $('#orderProductId');
  const orderResult = $('#orderResult');
  const historyRows = $('#historyRows');
  const toastEl = $('#toast');

  function productsBase() {
    const v = (productsBaseInput.value || '').trim();
    return v || DEFAULT_PRODUCTS_BASE;
  }

  function loadSettings() {
    const saved = localStorage.getItem(STORAGE_KEY);
    productsBaseInput.value = saved || DEFAULT_PRODUCTS_BASE;
  }

  function showToast(msg, isError) {
    toastEl.textContent = msg;
    toastEl.style.borderColor = isError ? 'rgba(240, 113, 120, 0.6)' : 'rgba(62, 207, 142, 0.45)';
    toastEl.classList.add('visible');
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => toastEl.classList.remove('visible'), 4200);
  }

  async function parseError(res) {
    const text = await res.text();
    try {
      const j = JSON.parse(text);
      return j.message || j.error || text || res.statusText;
    } catch {
      return text || res.statusText;
    }
  }

  function formatTs(raw) {
    if (raw == null) return '—';
    if (typeof raw === 'number') return new Date(raw).toLocaleString();
    if (typeof raw === 'string') return new Date(raw).toLocaleString();
    return String(raw);
  }

  async function refreshProducts() {
    productRows.innerHTML = '';
    orderProductId.innerHTML = '<option value="">— выберите товар —</option>';
    const base = productsBase().replace(/\/$/, '');
    let list;
    try {
      const res = await fetch(`${base}/products`);
      if (!res.ok) throw new Error(await parseError(res));
      list = await res.json();
    } catch (e) {
      showToast('Каталог: ' + (e.message || e), true);
      return;
    }
    if (!Array.isArray(list) || list.length === 0) {
      productRows.innerHTML = '<tr><td colspan="4" class="muted">Нет товаров — добавьте ниже.</td></tr>';
      return;
    }
    for (const p of list) {
      const tr = document.createElement('tr');
      tr.innerHTML =
        '<td class="mono">' +
        p.id +
        '</td><td>' +
        escapeHtml(p.name) +
        '</td><td>' +
        escapeHtml(String(p.price)) +
        '</td><td>' +
        escapeHtml(String(p.quantity ?? '')) +
        '</td>';
      productRows.appendChild(tr);

      const opt = document.createElement('option');
      opt.value = p.id;
      opt.textContent = p.name + ' (' + p.quantity + ' шт.)';
      orderProductId.appendChild(opt);
    }
    showToast('Каталог обновлён (' + list.length + ' поз.)');
  }

  function escapeHtml(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  $('#saveProductsBase').addEventListener('click', () => {
    localStorage.setItem(STORAGE_KEY, productsBaseInput.value.trim() || DEFAULT_PRODUCTS_BASE);
    showToast('Базовый URL API товаров сохранён');
    refreshProducts();
  });

  $('#refreshProducts').addEventListener('click', refreshProducts);

  $('#productForm').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const fd = new FormData(ev.target);
    const body = {
      name: fd.get('name').trim(),
      price: Number(fd.get('price')),
      quantity: Number(fd.get('quantity')),
    };
    const base = productsBase().replace(/\/$/, '');
    try {
      const res = await fetch(`${base}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (!res.ok) throw new Error(await parseError(res));
      ev.target.reset();
      const qInput = ev.target.querySelector('[name="quantity"]');
      if (qInput) qInput.value = '10';
      showToast('Товар создан');
      await refreshProducts();
    } catch (e) {
      showToast('Ошибка создания: ' + (e.message || e), true);
    }
  });

  $('#genCustomerId').addEventListener('click', () => {
    $('#customerId').value = crypto.randomUUID();
  });

  $('#orderForm').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    orderResult.hidden = true;
    const fd = new FormData(ev.target);
    const body = {
      customerId: fd.get('customerId').trim(),
      productId: fd.get('productId'),
      productQuantity: Number(fd.get('productQuantity')),
    };
    try {
      const res = await fetch('/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const text = await res.text();
      let data;
      try {
        data = JSON.parse(text);
      } catch {
        data = { raw: text };
      }
      if (!res.ok) {
        throw new Error(data.message || data.raw || res.statusText);
      }
      orderResult.hidden = false;
      orderResult.className = 'result ok';
      orderResult.innerHTML =
        '<strong>Заказ принят (202 Accepted)</strong><br>' +
        'ID заказа: <span class="mono">' +
        data.orderId +
        '</span><br>Статус: ' +
        escapeHtml(String(data.status)) +
        '<br><small>Скопируйте ID для раздела «История заказа».</small>';
      $('#historyOrderId').value = data.orderId;
      showToast('Заказ создан');
    } catch (e) {
      orderResult.hidden = false;
      orderResult.className = 'result err';
      orderResult.textContent = 'Ошибка: ' + (e.message || e);
      showToast(orderResult.textContent, true);
    }
  });

  $('#historyForm').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    historyRows.innerHTML = '';
    const orderId = $('#historyOrderId').value.trim();
    try {
      const res = await fetch('/orders/' + encodeURIComponent(orderId) + '/history');
      if (!res.ok) throw new Error(await parseError(res));
      const rows = await res.json();
      if (!Array.isArray(rows) || rows.length === 0) {
        historyRows.innerHTML = '<tr><td colspan="3">Записей пока нет.</td></tr>';
        showToast('История пуста');
        return;
      }
      for (const r of rows) {
        const tr = document.createElement('tr');
        tr.innerHTML =
          '<td class="mono">' +
          r.id +
          '</td><td>' +
          escapeHtml(String(r.status)) +
          '</td><td>' +
          formatTs(r.createdAt) +
          '</td>';
        historyRows.appendChild(tr);
      }
      showToast('Загружено записей: ' + rows.length);
    } catch (e) {
      showToast('История: ' + (e.message || e), true);
    }
  });

  if (!$('#customerId').value) {
    $('#customerId').value = crypto.randomUUID();
  }

  loadSettings();
  refreshProducts();
})();
