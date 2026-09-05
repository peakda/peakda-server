import { request, upload } from './api.js';
import { initLayout } from './layout.js';
import { badge, emptyTableRow, escapeHtml, formatDate, renderPagination, setLoading, showToast, confirmAction } from './ui.js';

const MAX_HIGHLIGHTS = 3;
const PAGE_SIZE = 10;

const searchForm = document.querySelector('#search-form');
const searchButton = document.querySelector('#search-button');
const queryInput = document.querySelector('#festival-query');
const resultsBody = document.querySelector('#festival-results');
const pagination = document.querySelector('#festival-pagination');
const summary = document.querySelector('#festival-summary');
const form = document.querySelector('#festival-form');
const saveButton = document.querySelector('#save-button');
const deleteButton = document.querySelector('#delete-button');
const addHighlightButton = document.querySelector('#add-highlight');
const highlightsRoot = document.querySelector('#highlights');
const heroImageKey = document.querySelector('#hero-image-key');
const heroImageFile = document.querySelector('#hero-image-file');
const heroPreview = document.querySelector('#hero-preview');

const fields = {
  status: document.querySelector('#status'),
  hook: document.querySelector('#hook'),
  periodNote: document.querySelector('#period-note'),
  placeNote: document.querySelector('#place-note'),
  admissionFee: document.querySelector('#admission-fee'),
  admissionFeeNote: document.querySelector('#admission-fee-note'),
  operatingHours: document.querySelector('#operating-hours'),
  operatingHoursNote: document.querySelector('#operating-hours-note'),
  caution: document.querySelector('#caution'),
  cautionNote: document.querySelector('#caution-note'),
  directionsTransit: document.querySelector('#directions-transit'),
  directionsCar: document.querySelector('#directions-car'),
};

const state = {
  page: 0,
  lastPage: null,
  selectedFestival: null,
  highlights: [],
  heroPreviewUrl: '',
};

await initLayout({
  title: '축제 에디토리얼 관리',
  description: '축제명 검색 기반 에디토리얼 저장',
});

searchForm.addEventListener('submit', searchFestivals);
resultsBody.addEventListener('click', selectFestival);
form.addEventListener('submit', saveEditorial);
deleteButton.addEventListener('click', deleteEditorial);
addHighlightButton.addEventListener('click', () => addHighlight());
heroImageFile.addEventListener('change', uploadHeroImage);
heroImageKey.addEventListener('input', () => {
  state.heroPreviewUrl = heroImageKey.value.trim().startsWith('http') ? heroImageKey.value.trim() : '';
  renderHeroPreview();
});
highlightsRoot.addEventListener('click', handleHighlightAction);

await loadFestivals(0);

async function searchFestivals(event) {
  event.preventDefault();
  await loadFestivals(0);
}

async function loadFestivals(page) {
  try {
    state.page = page;
    safeSetLoading(searchButton, true);
    setLoading(resultsBody, 3, '축제를 불러오는 중입니다.');
    const response = await request('/api/admin/festivals', {
      query: {
        q: queryInput.value.trim(),
        page,
        size: PAGE_SIZE,
      },
    });
    state.lastPage = response;
    renderResults(response);
    renderPagination(pagination, response, loadFestivals);
  } catch (error) {
    resultsBody.innerHTML = emptyTableRow(3, error.message || '축제 목록을 불러오지 못했습니다.');
    pagination.replaceChildren();
    toast(error.message || '축제 목록을 불러오지 못했습니다.', 'error');
  } finally {
    safeSetLoading(searchButton, false);
  }
}

function renderResults(page) {
  const festivals = page?.content || [];
  if (!festivals.length) {
    resultsBody.innerHTML = emptyTableRow(3, '검색 결과가 없습니다.');
    return;
  }

  resultsBody.innerHTML = festivals.map((festival) => {
    const selected = state.selectedFestival?.id === festival.id;
    return `
      <tr class="clickable-row ${selected ? 'is-selected' : ''}" data-festival-id="${festival.id}">
        <td>
          <div class="cell-primary">${escapeHtml(festival.name)}</div>
          <div class="cell-secondary">${escapeHtml(festival.venue || '-')} · ID ${escapeHtml(String(festival.id))}</div>
        </td>
        <td class="cell-nowrap">${escapeHtml(formatPeriod(festival))}</td>
        <td>${festival.hasEditorial ? badge(festival.editorialStatus) : badge('MISSING')}</td>
      </tr>
    `;
  }).join('');
}

async function selectFestival(event) {
  const row = event.target.closest('[data-festival-id]');
  if (!row) return;

  const festivalId = Number(row.dataset.festivalId);
  const selected = Array.from(resultsBody.querySelectorAll('[data-festival-id]'))
    .find((candidate) => Number(candidate.dataset.festivalId) === festivalId);
  state.selectedFestival = readFestivalFromRow(selected);
  resultsBody.querySelectorAll('tr').forEach((candidate) => candidate.classList.remove('is-selected'));
  row.classList.add('is-selected');
  renderSummary(state.selectedFestival);
  await loadEditorial(festivalId);
  window.history.replaceState(null, '', `/admin/festivals.html?festivalId=${encodeURIComponent(festivalId)}`);
}

function readFestivalFromRow(row) {
  const festivalId = Number(row.dataset.festivalId);
  const page = state.lastPage || { content: [] };
  return page.content.find((festival) => festival.id === festivalId) || { id: festivalId };
}

async function loadEditorial(festivalId) {
  try {
    const editorial = await request(`/api/admin/festivals/${encodeURIComponent(festivalId)}/editorial`);
    populateForm(editorial);
    toast('축제 에디토리얼을 불러왔습니다.');
  } catch (error) {
    if (error.status === 404 || error.code === 'FESTIVAL_EDITORIAL_NOT_FOUND') {
      populateForm(null);
      toast('기존 에디토리얼이 없어 새로 작성합니다.');
      return;
    }
    form.hidden = true;
    toast(error.message || '축제 에디토리얼을 불러오지 못했습니다.', 'error');
  }
}

function renderSummary(festival) {
  summary.hidden = false;
  summary.innerHTML = `
    <div class="panel-header">
      <div>
        <p class="eyebrow">Selected Festival</p>
        <h2>${escapeHtml(festival.name || `축제 ID ${festival.id}`)}</h2>
        <p>${escapeHtml(festival.venue || '-')}</p>
      </div>
      <div class="inline-actions">
        ${festival.hasEditorial ? badge(festival.editorialStatus) : badge('MISSING')}
      </div>
    </div>
    <div class="panel-body">
      <dl class="form-grid">
        <div><dt>축제 ID</dt><dd>${escapeHtml(String(festival.id))}</dd></div>
        <div><dt>기간</dt><dd>${escapeHtml(formatPeriod(festival))}</dd></div>
      </dl>
    </div>
  `;
}

function populateForm(editorial) {
  fields.status.value = editorial?.status || 'DRAFT';
  fields.hook.value = editorial?.hook || '';
  fields.periodNote.value = editorial?.periodNote || '';
  fields.placeNote.value = editorial?.placeNote || '';
  fields.admissionFee.value = editorial?.admissionFee || '';
  fields.admissionFeeNote.value = editorial?.admissionFeeNote || '';
  fields.operatingHours.value = editorial?.operatingHours || '';
  fields.operatingHoursNote.value = editorial?.operatingHoursNote || '';
  fields.caution.value = editorial?.caution || '';
  fields.cautionNote.value = editorial?.cautionNote || '';
  fields.directionsTransit.value = editorial?.directionsTransit || '';
  fields.directionsCar.value = editorial?.directionsCar || '';
  heroImageKey.value = editorial?.heroImageKey || '';
  state.heroPreviewUrl = editorial?.heroImagePreviewUrl || '';
  renderHeroPreview();

  state.highlights = (editorial?.highlights || []).slice(0, MAX_HIGHLIGHTS).map((highlight) => ({
    title: highlight.title || '',
    body: highlight.body || '',
  }));
  if (!state.highlights.length) addHighlight(false);
  renderHighlights();
  form.hidden = false;
}

async function saveEditorial(event) {
  event.preventDefault();
  if (!state.selectedFestival) return;
  syncHighlightsFromDom();

  const payload = {
    hook: nullIfBlank(fields.hook.value),
    periodNote: nullIfBlank(fields.periodNote.value),
    placeNote: nullIfBlank(fields.placeNote.value),
    admissionFee: nullIfBlank(fields.admissionFee.value),
    admissionFeeNote: nullIfBlank(fields.admissionFeeNote.value),
    operatingHours: nullIfBlank(fields.operatingHours.value),
    operatingHoursNote: nullIfBlank(fields.operatingHoursNote.value),
    caution: nullIfBlank(fields.caution.value),
    cautionNote: nullIfBlank(fields.cautionNote.value),
    directionsTransit: nullIfBlank(fields.directionsTransit.value),
    directionsCar: nullIfBlank(fields.directionsCar.value),
    heroImageKey: nullIfBlank(heroImageKey.value),
    status: fields.status.value,
    highlights: state.highlights.map((highlight) => ({
      title: highlight.title.trim(),
      body: highlight.body.trim(),
    })),
  };

  try {
    safeSetLoading(saveButton, true);
    await request(`/api/admin/festivals/${encodeURIComponent(state.selectedFestival.id)}/editorial`, {
      method: 'PUT',
      body: payload,
    });
    toast('축제 에디토리얼을 저장했습니다.');
    await loadEditorial(state.selectedFestival.id);
    await loadFestivals(state.page);
  } catch (error) {
    toast(error.message || '축제 에디토리얼 저장에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(saveButton, false);
  }
}

async function deleteEditorial() {
  if (!state.selectedFestival) return;
  const confirmed = await confirmAction({
    title: '축제 에디토리얼 삭제',
    message: '이 축제 에디토리얼을 삭제할까요?',
    confirmLabel: '삭제',
    danger: true,
  });
  if (!confirmed) return;

  try {
    safeSetLoading(deleteButton, true);
    await request(`/api/admin/festivals/${encodeURIComponent(state.selectedFestival.id)}/editorial`, {
      method: 'DELETE',
    });
    toast('축제 에디토리얼을 삭제했습니다.');
    populateForm(null);
    await loadFestivals(state.page);
  } catch (error) {
    toast(error.message || '축제 에디토리얼 삭제에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(deleteButton, false);
  }
}

async function uploadHeroImage() {
  const file = heroImageFile.files?.[0];
  if (!file) return;

  try {
    safeSetLoading(heroImageFile, true);
    const uploaded = await upload('/api/admin/curations/images', file);
    heroImageKey.value = uploaded.objectKey || '';
    state.heroPreviewUrl = uploaded.previewUrl || '';
    renderHeroPreview();
    toast('이미지를 업로드했습니다.');
  } catch (error) {
    toast(error.message || '이미지 업로드에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(heroImageFile, false);
    heroImageFile.value = '';
  }
}

function addHighlight(render = true) {
  syncHighlightsFromDom();
  if (state.highlights.length >= MAX_HIGHLIGHTS) {
    toast('주요 볼거리는 최대 3개까지 추가할 수 있습니다.', 'error');
    return;
  }
  state.highlights.push({ title: '', body: '' });
  if (render) renderHighlights();
}

function handleHighlightAction(event) {
  const button = event.target.closest('[data-action]');
  if (!button) return;

  syncHighlightsFromDom();
  const index = Number(button.dataset.index);
  if (button.dataset.action === 'remove') state.highlights.splice(index, 1);
  if (button.dataset.action === 'up' && index > 0) {
    [state.highlights[index - 1], state.highlights[index]] = [state.highlights[index], state.highlights[index - 1]];
  }
  if (button.dataset.action === 'down' && index < state.highlights.length - 1) {
    [state.highlights[index + 1], state.highlights[index]] = [state.highlights[index], state.highlights[index + 1]];
  }
  renderHighlights();
}

function renderHighlights() {
  highlightsRoot.innerHTML = state.highlights.map((highlight, index) => `
    <article class="editor-card" data-index="${index}">
      <div class="editor-card-header">
        <h3>볼거리 ${index + 1}</h3>
        <div class="inline-actions">
          <button class="button button-small button-secondary" type="button" data-action="up" data-index="${index}" ${index === 0 ? 'disabled' : ''}>위로</button>
          <button class="button button-small button-secondary" type="button" data-action="down" data-index="${index}" ${index >= state.highlights.length - 1 ? 'disabled' : ''}>아래로</button>
          <button class="button button-small button-danger" type="button" data-action="remove" data-index="${index}">삭제</button>
        </div>
      </div>
      <div class="editor-card-body form-grid">
        <label class="field">
          <span class="field__label">제목</span>
          <input class="field__control" data-field="title" maxlength="200" value="${escapeHtml(highlight.title)}" required>
        </label>
        <label class="field">
          <span class="field__label">설명</span>
          <textarea class="field__control" data-field="body" maxlength="3000" rows="4" required>${escapeHtml(highlight.body)}</textarea>
        </label>
      </div>
    </article>
  `).join('');
  addHighlightButton.disabled = state.highlights.length >= MAX_HIGHLIGHTS;
}

function syncHighlightsFromDom() {
  state.highlights = Array.from(highlightsRoot.querySelectorAll('.editor-card')).map((card) => ({
    title: card.querySelector('[data-field="title"]')?.value.trim() || '',
    body: card.querySelector('[data-field="body"]')?.value.trim() || '',
  }));
}

function renderHeroPreview() {
  if (!state.heroPreviewUrl) {
    heroPreview.innerHTML = '<p class="muted">이미지 미리보기가 없습니다. 저장값은 object key 그대로 유지됩니다.</p>';
    return;
  }
  heroPreview.innerHTML = `<img class="image-preview__image" src="${escapeHtml(state.heroPreviewUrl)}" alt="히어로 이미지 미리보기">`;
}

function formatPeriod(festival) {
  if (!festival.startsOn && !festival.endsOn) return '기간 정보 없음';
  return `${formatDate(festival.startsOn)} ~ ${formatDate(festival.endsOn)}`;
}

function nullIfBlank(value) {
  const trimmed = value?.trim() || '';
  return trimmed ? trimmed : null;
}

function safeSetLoading(element, loading) {
  element.disabled = loading;
  element.setAttribute('aria-busy', String(loading));
}

function toast(message, type = 'success') {
  try {
    showToast(message, type);
  } catch (_error) {
    window.alert(message);
  }
}
