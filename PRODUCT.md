# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

delegated: native Java Android shell with a local WebView interface, embedded Python/yt-dlp, FFmpeg, SQLite, and MediaStore.

## Users

The app is for one person using an Android phone to save public single-video links for personal offline use. The common situation is copying a link or share text from Douyin, YouTube, or X and returning to the app to save the video with as few steps as possible.

## Product Purpose

ShiYing accepts one Douyin work, YouTube video, or X video link, parses it on the phone, and saves the resulting media to local storage. Success means the user can paste, review the parsed result, download, and find the file without an account, cloud service, or third-party parsing website.

## Positioning

The product is a local-only mobile downloader: platform requests still use the network, but parsing, media merging, task history, and file publishing happen on the device. It does not route links through a project-owned or third-party parsing backend.

## Operating Context

- The user pastes a full share text or a URL.
- The user parses one Douyin video, YouTube video/Short, or X post containing a video.
- The user starts or cancels one download at a time.
- Completed files are stored in the system Downloads collection under `ShiYing`.
- The user may leave the app while a foreground download continues.
- The app must remain usable after the screen is reopened with previous history intact.

## Capabilities and Constraints

- Supported platforms: Douyin, YouTube, and X/Twitter.
- Douyin single videos and image galleries are supported; YouTube/X remain single-video only.
- Playlists, feeds, search pages, and multi-item non-Douyin posts are not supported.
- No account, cloud sync, analytics, ads, remote configuration, or third-party parser API.
- No in-app player, subtitles, editing, proxy UI, or cookie import in the first version.
- A link still requires internet. Downloaded files remain available offline.
- Downloads are limited to one active task and published through MediaStore.
- Missing login is reported as unsupported rather than attempting credentials.
- The app targets Android 8.0 and newer, with an arm64 release APK.

## Brand Commitments

- Product name: 拾影.
- Voice: direct, calm, and concrete Mandarin.
- The interface must remain local-first and avoid claims that parsing works without a network connection.
- Users must be told when a platform requires login, is region-restricted, or the link is not a supported single video.

## Evidence on Hand

- The approved product and implementation specifications are in `docs/superpowers/specs/2026-10-29-shiying-android-design.md` and `docs/superpowers/plans/2026-10-29-shiying-android.md`.
- There are no real user histories, testimonials, usage statistics, or provider partnerships to present as evidence.
- Platform names and sample links are factual references; no claim is made that all public videos are downloadable.

## Product Principles

1. Paste once, understand immediately, download with one confirmation.
2. Keep platform processing on the phone.
3. Name the real failure and the next recovery action.
4. Preserve completed files and history across app restarts.
5. Avoid features that do not improve the save-and-find workflow.

## Accessibility & Inclusion

- Touch targets must be at least 48×48 dp.
- Respect Android system font scaling and safe areas.
- Maintain readable contrast for all text and controls.
- System Back must close transient UI before leaving the app.
