---
name: ShiYing Android
description: A local-first single-video downloader for Android.
colors:
  paper-ground: "#F4F2EC"
  raised-paper: "#FFFDF8"
  quiet-paper: "#E9E5DC"
  ink: "#181816"
  muted-ink: "#66625B"
  night-capture: "#1C1D1C"
  projection-amber: "#E6AA4F"
  signal-green: "#177C59"
  warning-red: "#B7373F"
typography:
  headline:
    fontFamily: "HarmonyOS Sans SC, MiSans, PingFang SC, system-ui, sans-serif"
    fontSize: "2.35rem"
    fontWeight: 700
    lineHeight: 1.13
    letterSpacing: "-0.04em"
  title:
    fontFamily: "HarmonyOS Sans SC, MiSans, PingFang SC, system-ui, sans-serif"
    fontSize: "1.1rem"
    fontWeight: 700
    lineHeight: 1.35
  body:
    fontFamily: "HarmonyOS Sans SC, MiSans, PingFang SC, system-ui, sans-serif"
    fontSize: "0.94rem"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "HarmonyOS Sans SC, MiSans, PingFang SC, system-ui, sans-serif"
    fontSize: "0.76rem"
    fontWeight: 600
    lineHeight: 1.4
rounded:
  sm: "10px"
  md: "14px"
  lg: "20px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  page: "20px"
components:
  capture-input:
    backgroundColor: "{colors.night-capture}"
    textColor: "{colors.raised-paper}"
    rounded: "{rounded.lg}"
    padding: "18px"
  button-primary:
    backgroundColor: "{colors.projection-amber}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    height: "48px"
  button-download:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.paper-ground}"
    rounded: "{rounded.md}"
    height: "48px"
  history-row:
    backgroundColor: "{colors.raised-paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "12px 14px"
---

# Design System: ShiYing Android

## Overview

**Creative North Star: "口袋里的取景册"**

拾影不是一间视频网站，而是一本放在口袋里的小取景册：浅暖的纸面负责承载内容，深色的取景区负责聚焦链接，暖金色只标记“现在可以做的那一步”。界面始终像工具，不像内容平台的首页，用户打开后不需要先理解品牌故事。

整个界面保持单列、短路径和安静密度。视觉表达集中在一个取景框式预览、一个暖金主操作和一条明确进度线上；其余信息通过字号、留白和表面层级组织。动效只表达解析完成、进度变化和按压反馈，不装饰页面加载。

**Key Characteristics:**

- 暖灰纸面与近黑取景区形成单一强对比。
- 暖金是唯一主操作强调色，出现范围克制。
- 单列一屏，结果、当前任务和历史依次向下展开。
- 图标是同一套 1.75 描边 SVG，不使用 emoji。
- 实时支持浅色与深色系统主题，并尊重减少动效设置。

## Colors

调色板像纸张、暗室和投影灯，颜色只在操作或状态需要时出现。

### Primary

- **投射琥珀** (`#E6AA4F`): 解析链接、当前任务底色和下载进度。它是主操作色，不用于普通说明文字。

### Neutral

- **纸面暖灰** (`#F4F2EC`): 应用地面和页面背景。
- **浮起纸面** (`#FFFDF8`): 历史行与需要轻量分离的内容表面。
- **安静纸面** (`#E9E5DC`): 按压反馈、辅助磁贴和低层表面。
- **墨色** (`#181816`): 主文字和下载主按钮。
- **次级墨色** (`#66625B`): 说明文字、元数据和占位提示。
- **暗室取景** (`#1C1D1C`): 链接输入面板和视频预览的深色场域。

### Semantic

- **完成绿** (`#177C59`): 解析成功、完成状态。
- **提醒红** (`#B7373F`): 错误、失败和取消。

### Named Rules

**取景框规则。** 深色只承担输入与媒体预览，不把整页染黑；浅色地面必须始终可见。

**琥珀稀缺规则。** 暖金只用于主操作和当前任务，不承担普通文字、装饰边框或大面积背景。

## Typography

**Display Font:** Android 系统无衬线字体
**Body Font:** Android 系统无衬线字体

**Character:** 使用系统字体而不是远程字体，保证离线启动与中文覆盖。粗重标题负责快速定位，正文和元数据靠字号与灰度后退。

### Hierarchy

- **Headline** (700, 2.35rem, 1.13): 首页任务说明；桌面宽度提升到 3rem。
- **Title** (700, 1.1rem, 1.35): 区块标题和视频标题。
- **Body** (400, .94rem, 1.6): 解释性正文，最长约 34em。
- **Label** (600, .76rem, 1.4): 输入标签、状态和次级说明。
- **Numeric** (.75rem, tabular numerals): 进度百分比和时间。

## Layout

内容宽度限制在 760px，手机左右各 20px 留白。页面只有一列：品牌栏、任务说明、链接输入、解析结果、当前任务、最近保存。桌面宽度只增加预览高度和输入内边距，不改变任务顺序。

所有按钮最小高度 48px，历史行动区也保持 48×48 的触控目标。页面适配状态栏、手势区和系统字体缩放；不使用固定文字缩放。

## Elevation & Depth

系统采用色调分层而不是投影。纸面、浮起纸面和安静纸面承担层级，输入区通过深色材质进入前景。没有卡片阴影，也不使用边框叠加深色表面的“鬼影卡片”。

### Named Rules

**单层深度规则。** 一个区域只使用背景材质或边框中的一种方式表达层级，不同时堆叠。

## Shapes

主要容器使用 20px 圆角，输入与控制使用 14px，小型动作和历史按钮使用 10–12px。状态标签使用胶囊形状。取景框内部以细十字线作为固定几何，不绘制装饰性曲线或仿照片插画。

## Components

### Buttons

- **Shape:** 14px 圆角，最小高度 48px。
- **Primary:** 暖金底、近黑字，只用于“解析链接”。
- **Download:** 墨色底、纸面字，只用于“下载到手机”。
- **Ghost:** 透明底，在输入面板中使用高对比浅色，在页面中使用次级墨色。
- **Focus:** 3px 暖金外轮廓，偏移 2px。
- **Active:** 缩放到 .975，190ms 减速过渡。

### Inputs / Fields

- **Style:** 18px 深色外壳，内部文本区为 14px 圆角的深灰表面。
- **Focus:** 2px 暖金外轮廓。
- **Placeholder:** 保持可在深色背景上辨读的浅灰。

### Media Result

- **Shape:** 20px 圆角，最小高度 250px，桌面宽度提升到 340px。
- **Material:** 封面存在时铺满并覆盖底部暗色渐变；封面不可用时使用固定十字取景线。
- **Overlay:** 平台标签和标题固定在取景框内，标题最多占两到三行。

### Active Task

- **Style:** 暖金与浮起纸面混合的表面，不使用边框或投影。
- **Progress:** 6px 胶囊轨道，暖金填充，宽度变化 240ms。
- **Action:** 取消使用内联文字按钮，不打开确认弹窗。

### History Row

- **Shape:** 14px 圆角，72px 最小高度。
- **Content:** 左侧标题、平台、状态和时间；右侧只保留一个直接动作。
- **State:** 完成显示“打开”，失败或中断显示“重试”，活动任务只显示百分比。

## Do's and Don'ts

### Do:

- **Do** 让粘贴、解析、下载在同一个屏幕内闭环。
- **Do** 在深色输入与预览区域使用足够亮的正文和元数据。
- **Do** 保持 48px 触控区域和系统字体缩放。
- **Do** 用具体中文说明错误以及下一步。
- **Do** 在解析和下载期间保持已有内容与操作上下文可见。

### Don't:

- **Don't** 使用远程字体、远程图标或需要云端生成的界面资源。
- **Don't** 把“本地解析”解释成“不需要网络”。
- **Don't** 用多层卡片、彩色左边框或装饰性渐变掩盖信息结构。
- **Don't** 给同一屏叠加多个主按钮。
- **Don't** 使用弹窗处理可以直接内联完成的取消或错误。
