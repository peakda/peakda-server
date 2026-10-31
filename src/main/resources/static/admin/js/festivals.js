import { request, upload } from './api.js';
import { initLayout } from './layout.js';
import { badge, emptyTableRow, escapeHtml, formatDate, renderPagination, setLoading, showToast, confirmAction } from './ui.js';

const MAX_HIGHLIGHTS = 3;
const PAGE_SIZE = 10;
const TABLE_COLUMNS = 5;
const KIND_LABELS = { FESTIVAL: '축제·행사', TREE: '트리 명소' };
const PHASE_BADGES = {
  UPCOMING: ['badge-pending', '시작 전'],
  ONGOING: ['badge-success', '진행 중'],
  ENDING_SOON: ['badge-warning', '종료 임박'],
  ENDED: ['badge-neutral', '종료'],
};

const searchForm = document.querySelector('#search-form');
const searchButton = document.querySelector('#search-button');
const queryInput = document.querySelector('#festival-query');
const kindFilter = document.querySelector('#festival-kind');
const createButton = document.querySelector('#create-button');
const resultsBody = document.querySelector('#festival-results');
const pagination = document.querySelector('#festival-pagination');
const summary = document.querySelector('#festival-summary');
const baseForm = document.querySelector('#base-form');
const baseFormTitle = document.querySelector('#base-form-title');
const baseSaveButton = document.querySelector('#base-save-button');
const baseDeleteButton = document.querySelector('#base-delete-button');
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

const baseFields = {
  kind: document.querySelector('#base-kind'),
  name: document.querySelector('#base-name'),
  venue: document.querySelector('#base-venue'),
  startsOn: document.querySelector('#base-starts-on'),
  endsOn: document.querySelector('#base-ends-on'),
  roadAddress: document.querySelector('#base-road-address'),
  latitude: document.querySelector('#base-latitude'),
  longitude: document.querySelector('#base-longitude'),
  homepageUrl: document.querySelector('#base-homepage-url'),
  phoneNumber: document.querySelector('#base-phone-number'),
  hostOrganization: document.querySelector('#base-host-organization'),
  organizingInstitution: document.querySelector('#base-organizing-institution'),
};

const state = {
  page: 0,
  lastPage: null,
  selectedFestival: null,
  highlights: [],
  heroPreviewUrl: '',
};

await initLayout({
  title: '축제·트리 명소 관리',
  description: '축제·트리 명소 직접 등록과 에디토리얼 저장',
});

searchForm.addEventListener('submit', searchFestivals);
kindFilter.addEventListener('change', () => loadFestivals(0));
createButton.addEventListener('click', startCreate);
baseForm.addEventListener('submit', saveBase);
baseDeleteButton.addEventListener('click', deleteFestival);
resultsBody.addEventListener('click', handleResultAction);
form.addEventListener('submit', saveEditorial);
deleteButton.addEventListener('click', deleteEditorial);
addHighlightButton.addEventListener('click', addHighlight);
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
    setLoading(resultsBody, TABLE_COLUMNS, '축제를 불러오는 중입니다.');
    const response = await request('/api/admin/festivals', {
      query: {
        q: queryInput.value.trim(),
        kind: kindFilter.value,
        page,
        size: PAGE_SIZE,
      },
    });
    state.lastPage = response;
    if (state.selectedFestival) {
      const refreshed = response.content?.find((festival) => festival.id === state.selectedFestival.id);
      if (refreshed) {
        state.selectedFestival = refreshed;
        renderSummary(refreshed);
      }
    }
    renderResults(response);
    renderPagination(pagination, response, loadFestivals);
  } catch (error) {
    resultsBody.innerHTML = emptyTableRow(TABLE_COLUMNS, error.message || '축제 목록을 불러오지 못했습니다.');
    pagination.replaceChildren();
    toast(error.message || '축제 목록을 불러오지 못했습니다.', 'error');
  } finally {
    safeSetLoading(searchButton, false);
  }
}

function renderResults(page) {
  const festivals = page?.content || [];
  if (!festivals.length) {
    resultsBody.innerHTML = emptyTableRow(TABLE_COLUMNS, '검색 결과가 없습니다.');
    return;
  }

  resultsBody.innerHTML = festivals.map((festival) => {
    const selected = state.selectedFestival?.id === festival.id;
    return `
      <tr class="clickable-row ${selected ? 'is-selected' : ''}" data-festival-id="${festival.id}">
        <td>
          <div class="cell-primary">${escapeHtml(festival.name)} ${originTags(festival)}</div>
          <div class="cell-secondary">${escapeHtml(festival.venue || '-')} · ID ${escapeHtml(String(festival.id))}</div>
        </td>
        <td class="cell-nowrap">${escapeHtml(formatPeriod(festival))}</td>
        <td class="cell-nowrap">${phaseBadge(festival)}</td>
        <td>${festival.hasEditorial ? badge(festival.editorialStatus) : badge('MISSING')}</td>
        <td>${pinControl(festival)}</td>
      </tr>
    `;
  }).join('');
}

function pinControl(festival) {
  if (festival.pinnedAt) {
    return `<span class="badge">고정됨</span> <button class="button button-small button-secondary" type="button" data-action="unpin" data-festival-id="${festival.id}">해제</button>`;
  }
  if (festival.hasEditorial && festival.editorialStatus === 'PUBLISHED') {
    return `<button class="button button-small button-secondary" type="button" data-action="pin" data-festival-id="${festival.id}">고정</button>`;
  }
  return '<span class="muted">-</span>';
}

async function handleResultAction(event) {
  const button = event.target.closest('[data-action]');
  if (!button) {
    await selectFestival(event);
    return;
  }
  event.stopPropagation();
  const festivalId = Number(button.dataset.festivalId);
  const pinned = button.dataset.action === 'pin';
  try {
    button.disabled = true;
    await request(`/api/admin/festivals/${encodeURIComponent(festivalId)}/pin`, { method: pinned ? 'PUT' : 'DELETE' });
    toast(pinned ? '축제를 고정했습니다.' : '축제 고정을 해제했습니다.');
    await loadFestivals(state.page);
  } catch (error) {
    toast(error.message || '고정 상태를 변경하지 못했습니다.', 'error');
  } finally {
    button.disabled = false;
  }
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
  await openFestival(state.selectedFestival);
}

async function openFestival(festival) {
  renderSummary(festival);
  if (festival.source === 'MANUAL') {
    await loadBase(festival.id);
  } else {
    baseForm.hidden = true;
  }
  await loadEditorial(festival.id);
  window.history.replaceState(null, '', `/admin/festivals.html?festivalId=${encodeURIComponent(festival.id)}`);
}

/** 응답이 오는 사이 다른 축제를 고르면 늦게 온 응답으로 폼을 덮지 않는다. */
function isSelected(festivalId) {
  return state.selectedFestival?.id === festivalId;
}

function startCreate() {
  state.selectedFestival = null;
  resultsBody.querySelectorAll('tr').forEach((candidate) => candidate.classList.remove('is-selected'));
  summary.hidden = true;
  form.hidden = true;
  populateBase(null);
  baseFields.name.focus();
  window.history.replaceState(null, '', '/admin/festivals.html');
}

async function loadBase(festivalId) {
  try {
    const festival = await request(`/api/admin/festivals/${encodeURIComponent(festivalId)}`);
    if (!isSelected(festivalId)) return;
    populateBase(festival);
  } catch (error) {
    if (!isSelected(festivalId)) return;
    baseForm.hidden = true;
    toast(error.message || '축제 기본 정보를 불러오지 못했습니다.', 'error');
  }
}

function populateBase(festival) {
  const creating = !festival;
  baseFormTitle.textContent = creating ? '축제·트리 명소 직접 등록' : '기본 정보';
  baseSaveButton.textContent = creating ? '등록' : '기본 정보 저장';
  baseDeleteButton.hidden = creating;
  baseFields.kind.value = festival?.kind || 'TREE';
  baseFields.name.value = festival?.name || '';
  baseFields.venue.value = festival?.venue || '';
  baseFields.startsOn.value = festival?.startsOn || '';
  baseFields.endsOn.value = festival?.endsOn || '';
  baseFields.roadAddress.value = festival?.roadAddress || '';
  baseFields.latitude.value = festival?.latitude ?? '';
  baseFields.longitude.value = festival?.longitude ?? '';
  baseFields.homepageUrl.value = festival?.homepageUrl || '';
  baseFields.phoneNumber.value = festival?.phoneNumber || '';
  baseFields.hostOrganization.value = festival?.hostOrganization || '';
  baseFields.organizingInstitution.value = festival?.organizingInstitution || '';
  baseForm.hidden = false;
}

async function saveBase(event) {
  event.preventDefault();
  const payload = {
    kind: baseFields.kind.value,
    name: baseFields.name.value.trim(),
    venue: baseFields.venue.value.trim(),
    startsOn: baseFields.startsOn.value,
    endsOn: nullIfBlank(baseFields.endsOn.value),
    roadAddress: nullIfBlank(baseFields.roadAddress.value),
    latitude: numberOrNull(baseFields.latitude.value),
    longitude: numberOrNull(baseFields.longitude.value),
    homepageUrl: nullIfBlank(baseFields.homepageUrl.value),
    phoneNumber: nullIfBlank(baseFields.phoneNumber.value),
    hostOrganization: nullIfBlank(baseFields.hostOrganization.value),
    organizingInstitution: nullIfBlank(baseFields.organizingInstitution.value),
  };
  if (payload.endsOn && payload.endsOn < payload.startsOn) {
    toast('종료일은 시작일보다 빠를 수 없습니다.', 'error');
    return;
  }
  if ((payload.latitude === null) !== (payload.longitude === null)) {
    toast('위도와 경도는 함께 입력해야 합니다.', 'error');
    return;
  }

  const editing = state.selectedFestival?.source === 'MANUAL';
  try {
    safeSetLoading(baseSaveButton, true);
    if (editing) {
      await request(`/api/admin/festivals/${encodeURIComponent(state.selectedFestival.id)}`, { method: 'PUT', body: payload });
      toast('기본 정보를 저장했습니다.');
    } else {
      const { festivalId } = await request('/api/admin/festivals', { method: 'POST', body: payload });
      // 이후 조회가 실패해도 다시 저장하면 수정으로 가도록 선택 상태부터 잡는다.
      state.selectedFestival = { id: festivalId, source: 'MANUAL', name: payload.name };
      toast('직접 등록했습니다. 이어서 에디토리얼을 작성하세요.');
    }
  } catch (error) {
    toast(error.message || '기본 정보 저장에 실패했습니다.', 'error');
    safeSetLoading(baseSaveButton, false);
    return;
  }

  try {
    const festival = await request(`/api/admin/festivals/${encodeURIComponent(state.selectedFestival.id)}`);
    state.selectedFestival = { ...state.selectedFestival, ...festival };
    // 새 행은 id 내림차순 맨 앞이므로 등록 직후에는 첫 페이지를 다시 읽는다.
    await loadFestivals(editing ? state.page : 0);
    await openFestival(state.selectedFestival);
  } catch (error) {
    toast(error.message || '저장은 됐지만 화면을 새로 고치지 못했습니다.', 'error');
  } finally {
    safeSetLoading(baseSaveButton, false);
  }
}

async function deleteFestival() {
  if (state.selectedFestival?.source !== 'MANUAL') return;
  const confirmed = await confirmAction({
    title: '직접 등록 축제 삭제',
    message: `"${state.selectedFestival.name}"을(를) 에디토리얼·볼거리와 함께 삭제할까요?`,
    confirmLabel: '삭제',
    danger: true,
  });
  if (!confirmed) return;

  try {
    safeSetLoading(baseDeleteButton, true);
    await request(`/api/admin/festivals/${encodeURIComponent(state.selectedFestival.id)}`, { method: 'DELETE' });
    toast('축제를 삭제했습니다.');
    state.selectedFestival = null;
    summary.hidden = true;
    baseForm.hidden = true;
    form.hidden = true;
    window.history.replaceState(null, '', '/admin/festivals.html');
    await loadFestivals(state.page);
  } catch (error) {
    toast(error.message || '축제 삭제에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(baseDeleteButton, false);
  }
}

function readFestivalFromRow(row) {
  const festivalId = Number(row.dataset.festivalId);
  const page = state.lastPage || { content: [] };
  return page.content.find((festival) => festival.id === festivalId) || { id: festivalId };
}

async function loadEditorial(festivalId) {
  try {
    const editorial = await request(`/api/admin/festivals/${encodeURIComponent(festivalId)}/editorial`);
    if (!isSelected(festivalId)) return;
    populateForm(editorial);
    toast('축제 에디토리얼을 불러왔습니다.');
  } catch (error) {
    if (!isSelected(festivalId)) return;
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
        ${originTags(festival)}
        ${phaseBadge(festival)}
        ${festival.hasEditorial ? badge(festival.editorialStatus) : badge('MISSING')}
        ${festival.pinnedAt ? '<span class="badge">고정됨</span>' : ''}
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
  // addHighlight 는 화면의 이전 카드를 state 로 다시 읽으므로, 폼을 새로 채울 때는 빈 카드 하나만 직접 넣는다.
  if (!state.highlights.length) state.highlights.push({ title: '', body: '' });
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
    const uploaded = await upload('/api/admin/curations/images', file, { usage: 'HERO' });
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

function addHighlight() {
  syncHighlightsFromDom();
  if (state.highlights.length >= MAX_HIGHLIGHTS) {
    toast('주요 볼거리는 최대 3개까지 추가할 수 있습니다.', 'error');
    return;
  }
  state.highlights.push({ title: '', body: '' });
  renderHighlights();
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

function originTags(festival) {
  const tags = [];
  if (festival.kind === 'TREE') tags.push(`<span class="badge badge-app">${KIND_LABELS.TREE}</span>`);
  if (festival.source === 'MANUAL') tags.push('<span class="badge badge-admin">직접 등록</span>');
  return tags.join(' ');
}

function phaseBadge(festival) {
  const entry = PHASE_BADGES[festival.phase];
  if (!entry) return '<span class="muted">-</span>';
  const [className, label] = entry;
  const days = phaseDays(festival);
  return `<span class="badge ${className}">${escapeHtml(days === null ? label : `${label} ${days}`)}</span>`;
}

/** 시작 전은 시작까지, 종료 임박은 종료까지 남은 날을 D-N으로 붙인다. */
function phaseDays(festival) {
  const target = festival.phase === 'UPCOMING' ? festival.startsOn
    : festival.phase === 'ENDING_SOON' ? (festival.endsOn || festival.startsOn)
      : null;
  if (!target) return null;
  const now = new Date();
  const today = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  const [year, month, day] = target.split('-').map(Number);
  return `D-${Math.round((Date.UTC(year, month - 1, day) - today) / 86_400_000)}`;
}

function formatPeriod(festival) {
  if (!festival.startsOn && !festival.endsOn) return '기간 정보 없음';
  return `${formatDate(festival.startsOn)} ~ ${formatDate(festival.endsOn)}`;
}

function nullIfBlank(value) {
  const trimmed = value?.trim() || '';
  return trimmed ? trimmed : null;
}

function numberOrNull(value) {
  const trimmed = String(value ?? '').trim();
  return trimmed ? Number(trimmed) : null;
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
