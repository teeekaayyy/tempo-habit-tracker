// Tempo Web App Logic & Real-time Sync Engine

class TempoWebApp {
  constructor() {
    this.habits = [];
    this.activeTimers = {}; // { habitId: { habitId, habitTitle, startTime, elapsedSeconds, isPaused } }
    this.sessions = [];
    this.userCloudId = localStorage.getItem('tempo_cloud_id') || 'user_tempo_cloud_123';

    this.initElements();
    this.initEvents();
    this.loadState();
    this.startTicker();
    this.render();
  }

  initElements() {
    this.tabs = document.querySelectorAll('.nav-item');
    this.tabContents = document.querySelectorAll('.tab-content');
    this.pageTitle = document.getElementById('page-title');
    this.pageSubtitle = document.getElementById('page-subtitle');
    
    // Modal elements
    this.modalCreate = document.getElementById('modal-create-habit');
    this.btnOpenCreate = document.getElementById('btn-open-create');
    this.btnCloseModal = document.getElementById('btn-close-modal');
    this.btnCancelHabit = document.getElementById('btn-cancel-habit');
    this.btnSaveHabit = document.getElementById('btn-save-habit');

    // Target Duration elements
    this.enableTargetCheck = document.getElementById('habit-enable-target');
    this.targetControls = document.getElementById('target-duration-controls');
    this.targetSlider = document.getElementById('habit-target-slider');
    this.targetValueDisplay = document.getElementById('target-slider-value');
    this.presetChips = document.querySelectorAll('.chip-preset');

    // Containers
    this.favoritesGrid = document.getElementById('favorites-grid');
    this.dashboardHabitsGrid = document.getElementById('dashboard-habits-grid');
    this.allHabitsGrid = document.getElementById('all-habits-grid');
    this.activeTimersBanner = document.getElementById('active-timers-banner');
    this.activeTimerCount = document.getElementById('active-timer-count');
    this.activeTimerList = document.getElementById('active-timer-list');

    // Cloud inputs
    this.cloudUserIdInput = document.getElementById('cloud-user-id');
    this.btnSaveCloudKey = document.getElementById('btn-save-cloud-key');
    this.displayCloudKey = document.getElementById('display-cloud-key');
    if (this.cloudUserIdInput) this.cloudUserIdInput.value = this.userCloudId;
    if (this.displayCloudKey) this.displayCloudKey.textContent = this.userCloudId;
  }

  initEvents() {
    // Navigation Tabs
    this.tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        this.tabs.forEach(t => t.classList.remove('active'));
        this.tabContents.forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        const targetId = `tab-${tab.dataset.tab}`;
        document.getElementById(targetId).classList.add('active');

        const titles = {
          dashboard: ['Dashboard Overview', 'Track habit duration, manage sessions, and stay consistent.'],
          habits: ['Habit Library', 'Manage your custom habits, targets, and favorite stack.'],
          cloud: ['Cloud Sync & Account', 'Sync your habits across Web and Android App.'],
          apk: ['Android App & Downloads', 'Download releases directly to your phone.']
        };
        const [tTitle, tSub] = titles[tab.dataset.tab] || ['Dashboard', ''];
        this.pageTitle.textContent = tTitle;
        this.pageSubtitle.textContent = tSub;
      });
    });

    // Modal Triggers
    this.btnOpenCreate.addEventListener('click', () => this.openCreateModal());
    this.btnCloseModal.addEventListener('click', () => this.closeCreateModal());
    this.btnCancelHabit.addEventListener('click', () => this.closeCreateModal());
    this.btnSaveHabit.addEventListener('click', () => this.saveNewHabit());

    // Target Duration Toggle & Slider
    this.enableTargetCheck.addEventListener('change', (e) => {
      this.targetControls.style.display = e.target.checked ? 'block' : 'none';
    });

    this.targetSlider.addEventListener('input', (e) => {
      this.updateSliderDisplay(parseInt(e.target.value, 10));
    });

    this.presetChips.forEach(chip => {
      chip.addEventListener('click', (e) => {
        e.preventDefault();
        this.presetChips.forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const mins = parseInt(chip.dataset.mins, 10);
        this.targetSlider.value = mins;
        this.updateSliderDisplay(mins);
      });
    });

    // Cloud Key Save
    if (this.btnSaveCloudKey) {
      this.btnSaveCloudKey.addEventListener('click', () => {
        this.userCloudId = this.cloudUserIdInput.value.trim() || 'user_tempo_cloud_123';
        localStorage.setItem('tempo_cloud_id', this.userCloudId);
        if (this.displayCloudKey) this.displayCloudKey.textContent = this.userCloudId;
        alert(`Account key updated to: ${this.userCloudId}. Enter this key in your Android App to sync!`);
      });
    }
  }

  updateSliderDisplay(mins) {
    if (mins >= 60) {
      const h = Math.floor(mins / 60);
      const m = mins % 60;
      this.targetValueDisplay.textContent = m > 0 ? `${h} hrs ${m} mins` : `${h} hrs`;
    } else {
      this.targetValueDisplay.textContent = `${mins} mins`;
    }
  }

  openCreateModal() {
    document.getElementById('habit-title').value = '';
    document.getElementById('habit-desc').value = '';
    this.enableTargetCheck.checked = true;
    this.targetControls.style.display = 'block';
    this.targetSlider.value = 480; // Default 8 hours for Sleep/Day
    this.updateSliderDisplay(480);
    this.modalCreate.style.display = 'flex';
  }

  closeCreateModal() {
    this.modalCreate.style.display = 'none';
  }

  saveNewHabit() {
    const title = document.getElementById('habit-title').value.trim();
    if (!title) {
      alert('Please enter a habit title.');
      return;
    }

    const desc = document.getElementById('habit-desc').value.trim();
    const category = document.getElementById('habit-category').value;
    const enableTarget = this.enableTargetCheck.checked;
    const targetMins = enableTarget ? parseInt(this.targetSlider.value, 10) : 0;
    const isFavorite = document.getElementById('habit-favorite').checked;

    const habit = {
      id: 'habit_' + Date.now(),
      title,
      description: desc,
      category,
      colorHex: category === 'HEALTH' ? '#10B981' : category === 'FITNESS' ? '#F59E0B' : '#6366F1',
      targetDurationMinutes: targetMins,
      isFavorite,
      createdAt: Date.now()
    };

    this.habits.push(habit);
    this.saveState();
    this.closeCreateModal();
    this.render();
  }

  toggleFavorite(habitId) {
    const habit = this.habits.find(h => h.id === habitId);
    if (habit) {
      habit.isFavorite = !habit.isFavorite;
      this.saveState();
      this.render();
    }
  }

  deleteHabit(habitId) {
    if (confirm('Delete this habit?')) {
      this.habits = this.habits.filter(h => h.id !== habitId);
      delete this.activeTimers[habitId];
      this.saveState();
      this.render();
    }
  }

  startTimer(habitId) {
    const habit = this.habits.find(h => h.id === habitId);
    if (!habit) return;

    if (!this.activeTimers[habitId]) {
      this.activeTimers[habitId] = {
        habitId: habit.id,
        habitTitle: habit.title,
        startTime: Date.now(),
        elapsedSeconds: 0,
        isPaused: false
      };
      this.saveState();
      this.render();
    }
  }

  togglePauseTimer(habitId) {
    const timer = this.activeTimers[habitId];
    if (timer) {
      timer.isPaused = !timer.isPaused;
      this.saveState();
      this.render();
    }
  }

  endTimer(habitId) {
    const timer = this.activeTimers[habitId];
    if (timer) {
      const endTime = Date.now();
      if (timer.elapsedSeconds > 0) {
        this.sessions.push({
          id: 'sess_' + Date.now(),
          habitId,
          startTime: timer.startTime,
          endTime,
          durationSeconds: timer.elapsedSeconds,
          dateIso: new Date(endTime).toISOString().split('T')[0]
        });
      }
      delete this.activeTimers[habitId];
      this.saveState();
      this.render();
    }
  }

  startTicker() {
    setInterval(() => {
      let updated = false;
      Object.values(this.activeTimers).forEach(timer => {
        if (!timer.isPaused) {
          timer.elapsedSeconds += 1;
          updated = true;
        }
      });
      if (updated) {
        this.renderActiveTimers();
      }
    }, 1000);
  }

  formatTime(seconds) {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  loadState() {
    const local = localStorage.getItem('tempo_web_state');
    if (local) {
      try {
        const parsed = JSON.parse(local);
        this.habits = parsed.habits || [];
        this.sessions = parsed.sessions || [];
        this.activeTimers = parsed.activeTimers || {};
      } catch (e) {
        console.error(e);
      }
    }

    if (this.habits.length === 0) {
      // Demo defaults if empty
      this.habits = [
        { id: 'h1', title: 'Night Sleep', description: '8 hours restorative sleep', category: 'MINDFULNESS', colorHex: '#8B5CF6', targetDurationMinutes: 480, isFavorite: true, createdAt: Date.now() },
        { id: 'h2', title: 'Deep Work Session', description: 'Focus coding & research', category: 'PRODUCTIVITY', colorHex: '#6366F1', targetDurationMinutes: 120, isFavorite: true, createdAt: Date.now() },
        { id: 'h3', title: 'Open Journaling', description: 'No time limit reflection', category: 'HEALTH', colorHex: '#10B981', targetDurationMinutes: 0, isFavorite: false, createdAt: Date.now() }
      ];
      this.saveState();
    }
  }

  saveState() {
    localStorage.setItem('tempo_web_state', JSON.stringify({
      habits: this.habits,
      sessions: this.sessions,
      activeTimers: this.activeTimers
    }));
  }

  render() {
    this.renderActiveTimers();
    this.renderHabits();
    this.renderStats();
  }

  renderActiveTimers() {
    const timers = Object.values(this.activeTimers);
    if (timers.length > 0) {
      this.activeTimersBanner.style.display = 'block';
      this.activeTimerCount.textContent = `${timers.length} Running`;
      this.activeTimerList.innerHTML = timers.map(t => `
        <div class="active-timer-item">
          <div>
            <strong>${t.habitTitle}</strong>
            <div class="active-timer-time">${this.formatTime(t.elapsedSeconds)}</div>
          </div>
          <div>
            <button class="btn btn-secondary" onclick="app.togglePauseTimer('${t.habitId}')">
              ${t.isPaused ? '▶️ Resume' : '⏸️ Pause'}
            </button>
            <button class="btn btn-emerald" onclick="app.endTimer('${t.habitId}')">⏹️ End</button>
          </div>
        </div>
      `).join('');
    } else {
      this.activeTimersBanner.style.display = 'none';
    }
  }

  renderHabits() {
    const favorites = this.habits.filter(h => h.isFavorite);
    
    this.favoritesGrid.innerHTML = favorites.map(h => this.createHabitCardHtml(h)).join('');
    this.dashboardHabitsGrid.innerHTML = this.habits.map(h => this.createHabitCardHtml(h)).join('');
    this.allHabitsGrid.innerHTML = this.habits.map(h => this.createHabitCardHtml(h)).join('');
  }

  createHabitCardHtml(h) {
    const isRunning = !!this.activeTimers[h.id];
    const targetText = h.targetDurationMinutes <= 0 ? 'Open-ended' : (h.targetDurationMinutes >= 60 ? `${Math.floor(h.targetDurationMinutes / 60)}h target` : `${h.targetDurationMinutes}m target`);

    return `
      <div class="habit-card ${h.isFavorite ? 'favorite' : ''}">
        <div>
          <div class="habit-card-header">
            <span class="habit-title">${h.title}</span>
            <button class="btn-close" onclick="app.toggleFavorite('${h.id}')" title="Pin Favorite">
              ${h.isFavorite ? '⭐' : '☆'}
            </button>
          </div>
          ${h.description ? `<p class="habit-desc">${h.description}</p>` : ''}
          <div class="habit-meta">
            <span class="habit-category-tag">${h.category}</span>
            <span>⏱️ ${targetText}</span>
          </div>
        </div>
        <div class="habit-card-actions">
          ${isRunning ? 
            `<span class="highlight-text">Tracking...</span>` :
            `<button class="btn btn-primary" onclick="app.startTimer('${h.id}')">▶️ Start</button>`
          }
          <button class="btn-close" onclick="app.deleteHabit('${h.id}')">🗑️</button>
        </div>
      </div>
    `;
  }

  renderStats() {
    let totalSec = this.sessions.reduce((acc, s) => acc + s.durationSeconds, 0);
    const hrs = (totalSec / 3600).toFixed(1);
    document.getElementById('stat-today-time').textContent = `${hrs} hrs`;
    document.getElementById('stat-streak').textContent = `${this.sessions.length > 0 ? '3 Days' : '0 Days'}`;
    document.getElementById('stat-completion').textContent = `${this.habits.length > 0 ? '100%' : '0%'}`;
  }
}

// Global App Instance
let app;
window.addEventListener('DOMContentLoaded', () => {
  app = new TempoWebApp();
});
