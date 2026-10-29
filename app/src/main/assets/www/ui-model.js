(function installUiModel(global) {
  function formatDuration(ms) {
    const total = Math.max(0, Math.floor(Number(ms || 0) / 1000));
    const minutes = String(Math.floor(total / 60)).padStart(2, "0");
    const seconds = String(total % 60).padStart(2, "0");
    return `${minutes}:${seconds}`;
  }

  function statusLabel(status) {
    return {
      QUEUED: "等待中",
      PREPARING: "准备中",
      DOWNLOADING: "下载中",
      PROCESSING: "处理中",
      COMPLETED: "已完成",
      FAILED: "失败",
      CANCELED: "已取消",
      INTERRUPTED: "已中断",
    }[status] || "未知状态";
  }

  function platformLabel(platform) {
    return {
      DOUYIN: "抖音",
      YOUTUBE: "YouTube",
      X: "X",
    }[platform] || "视频";
  }

  global.ShiYingModel = {
    formatDuration,
    statusLabel,
    platformLabel,
  };
})(globalThis);
