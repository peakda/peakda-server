import { request, upload } from './api.js';
import { initLayout } from './layout.js';
import { escapeHtml, showToast, confirmAction } from './ui.js';

const MAX_CHAPTERS = 3;
const LAYOUTS = ['MAIN', 'RHYTHM_REVERSE', 'EDGE_BLEED'];

const params = new URLSearchParams(window.location.search);
let currentCurationId = params.get('id');

const form = document.querySelector('#curation-form');
const formTitle = document.querySelector('#form-title');
const deleteButton = document.querySelector('#delete-button');
const saveButton = document.querySelector('#save-button');
const addChapterButton = document.querySelector('#add-chapter');
const addRecommendationButton = document.querySelector('#add-recommendation');
const chaptersRoot = document.querySelector('#chapters');
const recommendationsRoot = document.querySelector('#recommendations');
const heroImageUrl = document.querySelector('#hero-image-url');
const heroImageFile = document.querySelector('#hero-image-file');
const heroPreview = document.querySelector('#hero-preview');

const fields = {
  status: document.querySelector('#status'),
  weekLabel: document.querySelector('#week-label'),
  weekStartDate: document.querySelector('#week-start-date'),
  weekEndDate: document.querySelector('#week-end-date'),
  title: document.querySelector('#title'),
  subtitle: document.querySelector('#subtitle'),
  intro: document.querySelector('#intro'),
  nextTeaserOverline: document.querySelector('#next-teaser-overline'),
  nextTeaserBody: document.querySelector('#next-teaser-body'),
};

const state = {
  chapters: [],
  recommendations: [],
};

await initLayout({
  title: currentCurationId ? '큐레이션 편집' : '큐레이션 작성',
  description: '큐레이션 전체 상태 저장',
});

form.addEventListener('submit', saveCuration);
deleteButton.addEventListener('click', deleteCuration);
addChapterButton.addEventListener('click', () => addChapter());
addRecommendationButton.addEventListener('click', () => addRecommendation());
heroImageFile.addEventListener('change', uploadHeroImage);
heroImageUrl.addEventListener('input', () => renderHeroPreview(heroImageUrl.value));

chaptersRoot.addEventListener('click', (event) => handleCollectionAction(event, 'chapters'));
recommendationsRoot.addEventListener('click', (event) => handleCollectionAction(event, 'recommendations'));

bootstrap();

async function bootstrap() {
  if (!currentCurationId) {
    setDefaultWeek();
    addChapter();
    addRecommendation();
    renderHeroPreview('');
    return;
  }

  formTitle.textContent = '큐레이션 편집';
  deleteButton.hidden = false;
  try {
    safeSetLoading(saveButton, true);
    const response = await request(`/api/admin/curations/${encodeURIComponent(currentCurationId)}`);
    populateForm(unwrap(response));
  } catch (error) {
    toast(error.message || '큐레이션 상세를 불러오지 못했습니다.', 'error');
  } finally {
    safeSetLoading(saveButton, false);
  }
}

function populateForm(curation) {
  fields.status.value = curation.status || 'DRAFT';
  fields.weekLabel.value = curation.weekLabel || '';
  fields.weekStartDate.value = curation.weekStartDate || '';
  fields.weekEndDate.value = curation.weekEndDate || '';
  fields.title.value = curation.title || '';
  fields.subtitle.value = curation.subtitle || '';
  fields.intro.value = curation.intro || '';
  fields.nextTeaserOverline.value = curation.nextTeaserOverline || '';
  fields.nextTeaserBody.value = curation.nextTeaserBody || '';
  heroImageUrl.value = curation.heroImageKey || '';
  renderHeroPreview(curation.heroImagePreviewUrl || curation.heroImageKey || '');

  state.chapters = (curation.chapters || []).slice(0, MAX_CHAPTERS).map((chapter) => ({
    layout: chapter.layout || 'MAIN',
    heading: chapter.heading || '',
    spotId: toInputNumber(chapter.spotId),
    placeName: chapter.placeName || '',
    latitude: toInputNumber(chapter.latitude),
    longitude: toInputNumber(chapter.longitude),
    photoUrl: chapter.photoKey || '',
    pullQuote: chapter.pullQuote || '',
    leadText: chapter.leadText || '',
    body: chapter.body || '',
    factNote: chapter.factNote || '',
  }));
  state.recommendations = (curation.recommendations || []).map((recommendation) => ({
    title: recommendation.title || '',
    spotId: toInputNumber(recommendation.spotId),
    placeName: recommendation.placeName || '',
    latitude: toInputNumber(recommendation.latitude),
    longitude: toInputNumber(recommendation.longitude),
    photoUrl: recommendation.photoKey || '',
    body: recommendation.body || '',
  }));

  if (!state.chapters.length) addChapter(false);
  if (!state.recommendations.length) addRecommendation(false);
  renderChapters();
  renderRecommendations();
}

async function saveCuration(event) {
  event.preventDefault();
  syncCollectionsFromDom();

  const payload = {
    weekStartDate: fields.weekStartDate.value,
    weekEndDate: fields.weekEndDate.value,
    weekLabel: fields.weekLabel.value.trim(),
    heroImageUrl: nullIfBlank(heroImageUrl.value),
    title: fields.title.value.trim(),
    subtitle: nullIfBlank(fields.subtitle.value),
    intro: nullIfBlank(fields.intro.value),
    nextTeaserOverline: nullIfBlank(fields.nextTeaserOverline.value),
    nextTeaserBody: nullIfBlank(fields.nextTeaserBody.value),
    status: fields.status.value,
    chapters: state.chapters.map(chapterPayload),
    recommendations: state.recommendations.map(recommendationPayload),
  };

  try {
    safeSetLoading(saveButton, true);
    const response = await request('/api/admin/curations', {
      method: 'PUT',
      body: payload,
    });
    const saved = unwrap(response);
    toast('큐레이션을 저장했습니다.');
    if (!currentCurationId && saved?.curationId) {
      currentCurationId = String(saved.curationId);
      window.history.replaceState(null, '', `/admin/curation-edit.html?id=${encodeURIComponent(currentCurationId)}`);
      deleteButton.hidden = false;
    }
  } catch (error) {
    toast(error.message || '큐레이션 저장에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(saveButton, false);
  }
}

async function deleteCuration() {
  if (!currentCurationId) return;
  const confirmed = await confirmAction({
    title: '큐레이션 삭제',
    message: '이 큐레이션을 삭제할까요?',
    confirmLabel: '삭제',
    danger: true,
  });
  if (!confirmed) return;

  try {
    safeSetLoading(deleteButton, true);
    await request(`/api/admin/curations/${encodeURIComponent(currentCurationId)}`, { method: 'DELETE' });
    toast('큐레이션을 삭제했습니다.');
    window.location.href = '/admin/curations.html';
  } catch (error) {
    toast(error.message || '큐레이션 삭제에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(deleteButton, false);
  }
}

async function uploadHeroImage() {
  const file = heroImageFile.files?.[0];
  if (!file) return;

  try {
    safeSetLoading(heroImageFile, true);
    const response = await upload('/api/admin/curations/images', file);
    const uploaded = unwrap(response);
    heroImageUrl.value = uploaded.objectKey || '';
    renderHeroPreview(uploaded.previewUrl || uploaded.objectKey || '');
    toast('이미지를 업로드했습니다.');
  } catch (error) {
    toast(error.message || '이미지 업로드에 실패했습니다.', 'error');
  } finally {
    safeSetLoading(heroImageFile, false);
    heroImageFile.value = '';
  }
}

function addChapter(render = true) {
  syncCollectionsFromDom();
  if (state.chapters.length >= MAX_CHAPTERS) {
    toast('챕터는 최대 3개까지 추가할 수 있습니다.', 'error');
    return;
  }
  state.chapters.push({
    layout: 'MAIN',
    heading: '',
    spotId: '',
    placeName: '',
    latitude: '',
    longitude: '',
    photoUrl: '',
    pullQuote: '',
    leadText: '',
    body: '',
    factNote: '',
  });
  if (render) renderChapters();
}

function addRecommendation(render = true) {
  syncCollectionsFromDom();
  state.recommendations.push({
    title: '',
    spotId: '',
    placeName: '',
    latitude: '',
    longitude: '',
    photoUrl: '',
    body: '',
  });
  if (render) renderRecommendations();
}

function handleCollectionAction(event, collection) {
  const button = event.target.closest('[data-action]');
  if (!button) return;

  syncCollectionsFromDom();
  const index = Number(button.dataset.index);
  const items = state[collection];
  if (button.dataset.action === 'remove') items.splice(index, 1);
  if (button.dataset.action === 'up' && index > 0) [items[index - 1], items[index]] = [items[index], items[index - 1]];
  if (button.dataset.action === 'down' && index < items.length - 1) {
    [items[index + 1], items[index]] = [items[index], items[index + 1]];
  }
  if (collection === 'chapters') renderChapters();
  if (collection === 'recommendations') renderRecommendations();
}

function renderChapters() {
  chaptersRoot.innerHTML = state.chapters.map((chapter, index) => `
    <article class="editor-card" data-collection="chapters" data-index="${index}">
      <div class="editor-card-header">
        <h3>챕터 ${index + 1}</h3>
        ${controlButtons(index, state.chapters.length)}
      </div>
      <div class="editor-card-body form-grid">
        <label class="field">
          <span class="field__label">레이아웃</span>
          <select class="field__control" data-field="layout" required>
            ${LAYOUTS.map((layout) => `<option value="${layout}" ${chapter.layout === layout ? 'selected' : ''}>${layout}</option>`).join('')}
          </select>
        </label>
        ${inputField('heading', '헤딩', chapter.heading, true, 100)}
        ${inputField('spotId', '스팟 ID', chapter.spotId, false, null, 'number')}
        ${inputField('placeName', '장소명', chapter.placeName, true, 200)}
        ${inputField('latitude', '위도', chapter.latitude, false, null, 'number', 'any')}
        ${inputField('longitude', '경도', chapter.longitude, false, null, 'number', 'any')}
      </div>
      <div class="editor-card-body form-grid">
        ${inputField('photoUrl', '사진 URL 또는 objectKey', chapter.photoUrl, false, 2000)}
        ${textareaField('pullQuote', '풀쿼트', chapter.pullQuote, false, 500, 2)}
        ${textareaField('leadText', '리드 텍스트', chapter.leadText, false, 1000, 3)}
        ${textareaField('body', '본문', chapter.body, true, 5000, 5)}
        ${textareaField('factNote', '운영·요금·주의사항', chapter.factNote, false, 1000, 3)}
      </div>
    </article>
  `).join('');
  addChapterButton.disabled = state.chapters.length >= MAX_CHAPTERS;
}

function renderRecommendations() {
  recommendationsRoot.innerHTML = state.recommendations.map((recommendation, index) => `
    <article class="editor-card" data-collection="recommendations" data-index="${index}">
      <div class="editor-card-header">
        <h3>추천 ${index + 1}</h3>
        ${controlButtons(index, state.recommendations.length)}
      </div>
      <div class="editor-card-body form-grid">
        ${inputField('title', '제목', recommendation.title, true, 200)}
        ${inputField('spotId', '스팟 ID', recommendation.spotId, false, null, 'number')}
        ${inputField('placeName', '장소명', recommendation.placeName, true, 200)}
        ${inputField('latitude', '위도', recommendation.latitude, false, null, 'number', 'any')}
        ${inputField('longitude', '경도', recommendation.longitude, false, null, 'number', 'any')}
      </div>
      <div class="editor-card-body form-grid">
        ${inputField('photoUrl', '사진 URL 또는 objectKey', recommendation.photoUrl, false, 2000)}
        ${textareaField('body', '설명', recommendation.body, true, 3000, 4)}
      </div>
    </article>
  `).join('');
}

function syncCollectionsFromDom() {
  state.chapters = readCollection(chaptersRoot, readChapter);
  state.recommendations = readCollection(recommendationsRoot, readRecommendation);
}

function readCollection(root, mapper) {
  return Array.from(root.querySelectorAll('.editor-card')).map(mapper);
}

function readChapter(card) {
  return {
    layout: readField(card, 'layout') || 'MAIN',
    heading: readField(card, 'heading'),
    spotId: readField(card, 'spotId'),
    placeName: readField(card, 'placeName'),
    latitude: readField(card, 'latitude'),
    longitude: readField(card, 'longitude'),
    photoUrl: readField(card, 'photoUrl'),
    pullQuote: readField(card, 'pullQuote'),
    leadText: readField(card, 'leadText'),
    body: readField(card, 'body'),
    factNote: readField(card, 'factNote'),
  };
}

function readRecommendation(card) {
  return {
    title: readField(card, 'title'),
    spotId: readField(card, 'spotId'),
    placeName: readField(card, 'placeName'),
    latitude: readField(card, 'latitude'),
    longitude: readField(card, 'longitude'),
    photoUrl: readField(card, 'photoUrl'),
    body: readField(card, 'body'),
  };
}

function readField(card, field) {
  return card.querySelector(`[data-field="${field}"]`)?.value.trim() || '';
}

function chapterPayload(chapter) {
  return {
    layout: chapter.layout,
    heading: chapter.heading.trim(),
    spotId: numberOrNull(chapter.spotId),
    placeName: chapter.placeName.trim(),
    latitude: numberOrNull(chapter.latitude),
    longitude: numberOrNull(chapter.longitude),
    photoUrl: nullIfBlank(chapter.photoUrl),
    pullQuote: nullIfBlank(chapter.pullQuote),
    leadText: nullIfBlank(chapter.leadText),
    body: chapter.body.trim(),
    factNote: nullIfBlank(chapter.factNote),
  };
}

function recommendationPayload(recommendation) {
  return {
    title: recommendation.title.trim(),
    spotId: numberOrNull(recommendation.spotId),
    placeName: recommendation.placeName.trim(),
    latitude: numberOrNull(recommendation.latitude),
    longitude: numberOrNull(recommendation.longitude),
    photoUrl: nullIfBlank(recommendation.photoUrl),
    body: recommendation.body.trim(),
  };
}

function controlButtons(index, length) {
  return `
    <div class="inline-actions">
      <button class="button button-small button-secondary" type="button" data-action="up" data-index="${index}" ${index === 0 ? 'disabled' : ''}>위로</button>
      <button class="button button-small button-secondary" type="button" data-action="down" data-index="${index}" ${index >= length - 1 ? 'disabled' : ''}>아래로</button>
      <button class="button button-small button-danger" type="button" data-action="remove" data-index="${index}">삭제</button>
    </div>
  `;
}

function inputField(name, label, value, required = false, maxlength = null, type = 'text', step = null) {
  const max = maxlength ? ` maxlength="${maxlength}"` : '';
  const stepAttr = step ? ` step="${step}"` : '';
  return `
    <label class="field">
      <span class="field__label">${label}</span>
      <input class="field__control" data-field="${name}" type="${type}" value="${escapeHtml(String(value ?? ''))}"${max}${stepAttr}${required ? ' required' : ''}>
    </label>
  `;
}

function textareaField(name, label, value, required = false, maxlength = null, rows = 3) {
  const max = maxlength ? ` maxlength="${maxlength}"` : '';
  return `
    <label class="field">
      <span class="field__label">${label}</span>
      <textarea class="field__control" data-field="${name}" rows="${rows}"${max}${required ? ' required' : ''}>${escapeHtml(String(value ?? ''))}</textarea>
    </label>
  `;
}

function renderHeroPreview(src) {
  if (!src) {
    heroPreview.innerHTML = '<p class="muted">이미지 미리보기가 없습니다.</p>';
    return;
  }
  heroPreview.innerHTML = `<img class="image-preview__image" src="${escapeHtml(src)}" alt="히어로 이미지 미리보기">`;
}

function setDefaultWeek() {
  const today = kstToday();
  const day = today.getUTCDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  const monday = addDays(today, mondayOffset);
  const sunday = addDays(monday, 6);
  fields.weekStartDate.value = toDateInput(monday);
  fields.weekEndDate.value = toDateInput(sunday);
  fields.weekLabel.value = `${monday.getUTCMonth() + 1}월 ${weekOfMonth(monday)}주차`;
}

function kstToday() {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return new Date(Date.UTC(Number(value.year), Number(value.month) - 1, Number(value.day)));
}

function addDays(date, days) {
  const next = new Date(date);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

function toDateInput(date) {
  return date.toISOString().slice(0, 10);
}

function weekOfMonth(date) {
  return Math.ceil(date.getUTCDate() / 7);
}

function unwrap(response) {
  return response && Object.prototype.hasOwnProperty.call(response, 'data') ? response.data : response;
}

function nullIfBlank(value) {
  const trimmed = value?.trim() || '';
  return trimmed ? trimmed : null;
}

function numberOrNull(value) {
  const trimmed = String(value ?? '').trim();
  return trimmed ? Number(trimmed) : null;
}

function toInputNumber(value) {
  return value === null || value === undefined ? '' : String(value);
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
