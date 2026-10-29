(() => {
  const model = window.ShiYingModel;
  const bridge = window.ShiYingNative;
  const state = {
    parsed: null,
    requestId: "",
    activeTaskId: "",
    history: [],
    parsing: false,
  };

  const elements = {
    form: document.querySelector("#parseForm"),
    input: document.querySelector("#urlInput"),
    paste: document.querySelector("#pasteButton"),
    parse: document.querySelector("#parseButton"),
    notice: document.querySelector("#notice"),
    result: document.querySelector("#result"),
    platform: document.querySelector("#platformTag"),
    title: document.querySelector("#mediaTitle"),
    author: document.querySelector("#mediaAuthor"),
    duration: document.querySelector("#mediaDuration"),
    resultHint: document.querySelector("#resultHint"),
    downloadLabel: document.querySelector("#downloadLabel"),
    download: document.querySelector("#downloadButton"),
    active: document.querySelector("#activeTask"),
    activeLabel: document.querySelector("#activeLabel"),
    activeTitle: document.querySelector("#activeTitle"),
    progressFill: document.querySelector("#progressFill"),
    progressValue: document.querySelector("#progressValue"),
    cancel: document.querySelector("#cancelButton"),
    history: document.querySelector("#historyList"),
    refresh: document.querySelector("#refreshButton"),
    openDownloads: document.querySelector("#openDownloads"),
  };

  function setNotice(message = "", type = "") {
    elements.notice.textContent = message;
    elements.notice.className = `notice${type ? ` ${type}` : ""}`;
  }

  function setParsing(parsing) {
    state.parsing = parsing;
    elements.parse.disabled = parsing;
    elements.parse.setAttribute("aria-busy", String(parsing));
    elements.parse.querySelector("span").textContent = parsing ? "解析中…" : "解析链接";
  }

  function parseCurrentInput(event) {
    event.preventDefault();
    const raw = elements.input.value.trim();
    if (!raw) {
      setNotice("先粘贴一个视频链接", "error");
      elements.input.focus();
      return;
    }
    if (!bridge) {
      setNotice("请在拾影 Android App 中解析链接", "error");
      return;
    }
    setNotice("正在读取视频信息…");
    setParsing(true);
    elements.result.hidden = true;
    state.requestId = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    bridge.parse(raw, state.requestId);
  }

  window.onNativeParse = (requestId, json) => {
    if (requestId !== state.requestId) {
      return;
    }
    setParsing(false);
    let response;
    try {
      response = JSON.parse(json);
    } catch {
      setNotice("解析结果无法读取，请重试", "error");
      return;
    }
    if (!response.ok) {
      setNotice(response.message || "解析失败，请重试", "error");
      return;
    }
    state.parsed = response.media;
    renderResult();
    setNotice("链接已解析，确认后下载到“下载/拾影”", "success");
  };

  function pasteLink() {
    const value = bridge && bridge.readClipboard
      ? bridge.readClipboard()
      : "";
    if (!value) {
      setNotice("剪贴板里没有可粘贴的链接", "error");
      elements.input.focus();
      return;
    }
    elements.input.value = value;
    elements.input.focus();
    elements.input.setSelectionRange(value.length, value.length);
    setNotice("已粘贴分享内容");
  }

  function renderResult() {
    const media = state.parsed;
    if (!media) {
      elements.result.hidden = true;
      return;
    }
    elements.title.textContent = media.title || "未命名视频";
    elements.author.textContent = media.author || "未知作者";
    elements.duration.textContent = model.formatDuration(media.durationMs);
    const imageMode = media.kind === "IMAGES";
    const imageCount = Math.max(0, Number(media.imageCount || 0));
    elements.platform.textContent = imageMode
      ? `${model.platformLabel(media.platform)} · ${imageCount} 张`
      : model.platformLabel(media.platform);
    elements.duration.textContent = imageMode
      ? `${imageCount} 张`
      : model.formatDuration(media.durationMs);
    elements.resultHint.textContent = imageMode
      ? "原图保存到“图片/ShiYing”"
      : "自动选择兼容性较好的最高 1080p MP4";
    elements.downloadLabel.textContent = imageMode
      ? `保存 ${imageCount} 张图片`
      : "下载到手机";

    const existingCover = elements.result.querySelector("#cover");
    if (!media.coverUrl) {
      existingCover?.remove();
    }
    if (media.coverUrl) {
      const cover = existingCover || document.createElement("img");
      cover.id = "cover";
      cover.alt = `${media.title || "视频"} 封面`;
      cover.addEventListener("error", () => cover.remove(), { once: true });
      cover.src = media.coverUrl;
      if (!existingCover) {
        elements.result.querySelector(".media-frame").prepend(cover);
      }
    }
    elements.result.hidden = false;
    elements.result.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  function startDownload() {
    if (!state.parsed || !bridge) {
      return;
    }
    const response = parseJson(bridge.startDownload(JSON.stringify(state.parsed)));
    if (!response || !response.ok) {
      setNotice((response && response.message) || "无法开始下载", "error");
      return;
    }
    state.activeTaskId = response.taskId;
    elements.download.disabled = true;
    setNotice("已开始下载，可以离开拾影，任务会继续");
    refreshHistory();
    startPolling();
  }

  function cancelDownload() {
    if (!state.activeTaskId || !bridge) {
      return;
    }
    bridge.cancel(state.activeTaskId);
    setNotice("正在取消下载…");
  }

  function retryTask(taskId) {
    if (!bridge) {
      return;
    }
    const response = parseJson(bridge.retry(taskId));
    if (!response || !response.ok) {
      setNotice((response && response.message) || "重试失败", "error");
      return;
    }
    state.activeTaskId = response.taskId;
    setNotice("已重新开始下载");
    refreshHistory();
    startPolling();
  }

  function openFile(uri) {
    if (bridge && uri) {
      bridge.openFile(uri);
    }
  }

  function openDownloads() {
    if (bridge) {
      bridge.openDownloads();
    }
  }

  function refreshHistory() {
    if (!bridge) {
      renderHistory();
      return;
    }
    try {
      state.history = JSON.parse(bridge.history());
    } catch {
      state.history = [];
    }
    renderHistory();
    const active = state.history.find((task) => task.id === state.activeTaskId);
    renderActive(active);
  }

  function renderActive(task) {
    if (!task) {
      state.activeTaskId = "";
      elements.active.hidden = true;
      elements.download.disabled = false;
      return;
    }
    elements.active.hidden = false;
    elements.activeTitle.textContent = task.title;
    elements.activeLabel.textContent = model.statusLabel(task.status);
    const progress = Math.max(0, Math.min(100, task.progress || 0));
    elements.progressFill.style.width = `${progress}%`;
    elements.progressValue.textContent = `${progress}%`;

    const terminal = ["COMPLETED", "FAILED", "CANCELED", "INTERRUPTED"].includes(task.status);
    elements.cancel.hidden = terminal;
    elements.download.disabled = !terminal;
    if (task.status === "COMPLETED") {
      setNotice("视频已保存到“下载/拾影”", "success");
    } else if (task.status === "FAILED" || task.status === "INTERRUPTED") {
      setNotice(task.errorMessage || "下载没有完成", "error");
    } else if (task.status === "CANCELED") {
      setNotice("下载已取消");
    }
  }

  function renderHistory() {
    elements.history.replaceChildren();
    if (!state.history.length) {
      const empty = document.createElement("div");
      empty.className = "empty-history";
      const title = document.createElement("strong");
      title.textContent = "还没有下载记录";
      const copy = document.createElement("span");
      copy.textContent = "解析一条公开视频链接，完成后的文件会出现在这里。";
      empty.append(title, copy);
      elements.history.append(empty);
      return;
    }

    for (const task of state.history) {
      const row = document.createElement("article");
      row.className = "history-row";

      const titleBlock = document.createElement("div");
      titleBlock.className = "history-title";
      const title = document.createElement("h3");
      title.textContent = task.title || "未命名视频";
      const meta = document.createElement("div");
      meta.className = "history-meta";
      const platform = document.createElement("span");
      platform.textContent = model.platformLabel(task.platform);
      const status = document.createElement("span");
      status.className = `status ${String(task.status).toLowerCase()}`;
      status.textContent = model.statusLabel(task.status);
      const time = document.createElement("span");
      time.textContent = formatTime(task.updatedAt);
      meta.append(platform, status, time);
      titleBlock.append(title, meta);

      const actions = document.createElement("div");
      actions.className = "history-actions";
      if (task.status === "COMPLETED" && task.localUri) {
        const open = document.createElement("button");
        open.type = "button";
        open.textContent = "打开";
        open.addEventListener("click", () => openFile(task.localUri));
        actions.append(open);
      } else if (task.status === "FAILED" || task.status === "INTERRUPTED") {
        const retry = document.createElement("button");
        retry.type = "button";
        retry.textContent = "重试";
        retry.addEventListener("click", () => retryTask(task.id));
        actions.append(retry);
      } else if (["QUEUED", "PREPARING", "DOWNLOADING", "PROCESSING"].includes(task.status)) {
        const progress = document.createElement("span");
        progress.className = "status";
        progress.textContent = `${task.progress || 0}%`;
        actions.append(progress);
      }

      row.append(titleBlock, actions);
      elements.history.append(row);
    }
  }

  function formatTime(milliseconds) {
    try {
      return new Intl.DateTimeFormat("zh-CN", {
        month: "numeric",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }).format(new Date(milliseconds));
    } catch {
      return "";
    }
  }

  function parseJson(value) {
    try {
      return JSON.parse(value);
    } catch {
      return null;
    }
  }

  let pollingTimer = 0;
  function startPolling() {
    stopPolling();
    pollingTimer = window.setInterval(() => {
      refreshHistory();
      const active = state.history.find((task) => task.id === state.activeTaskId);
      if (!active || ["COMPLETED", "FAILED", "CANCELED", "INTERRUPTED"].includes(active.status)) {
        stopPolling();
      }
    }, 700);
  }

  function stopPolling() {
    if (pollingTimer) {
      window.clearInterval(pollingTimer);
      pollingTimer = 0;
    }
  }

  elements.form.addEventListener("submit", parseCurrentInput);
  elements.paste.addEventListener("click", pasteLink);
  elements.download.addEventListener("click", startDownload);
  elements.cancel.addEventListener("click", cancelDownload);
  elements.refresh.addEventListener("click", refreshHistory);
  elements.openDownloads.addEventListener("click", openDownloads);
  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) {
      refreshHistory();
    }
  });

  refreshHistory();
  if (new URLSearchParams(window.location.search).has("demo")) {
    state.parsed = {
      platform: "DOUYIN",
      title: "从城市边缘走到落日下面",
      author: "小林散步",
      coverUrl: "",
      durationMs: 75000,
    };
    state.history = [
      {
        id: "demo-active",
        platform: "YOUTUBE",
        title: "把周末剪成一段安静的视频",
        status: "DOWNLOADING",
        progress: 64,
        updatedAt: Date.now(),
        localUri: "",
      },
      {
        id: "demo-done",
        platform: "X",
        title: "街角的一段雨声",
        status: "COMPLETED",
        progress: 100,
        updatedAt: Date.now() - 86_400_000,
        localUri: "demo",
      },
    ];
    state.activeTaskId = "demo-active";
    renderResult();
    renderActive(state.history[0]);
    renderHistory();
    setNotice("演示数据", "");
  }
})();
