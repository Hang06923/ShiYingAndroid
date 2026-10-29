import test from "node:test";
import assert from "node:assert/strict";

await import("../app/src/main/assets/www/ui-model.js");

const { formatDuration, statusLabel, platformLabel } = globalThis.ShiYingModel;

test("formats video duration", () => {
  assert.equal(formatDuration(125000), "02:05");
});

test("shows Chinese status labels", () => {
  assert.equal(statusLabel("DOWNLOADING"), "下载中");
  assert.equal(statusLabel("INTERRUPTED"), "已中断");
});

test("shows supported platform names", () => {
  assert.equal(platformLabel("DOUYIN"), "抖音");
  assert.equal(platformLabel("YOUTUBE"), "YouTube");
  assert.equal(platformLabel("X"), "X");
});
