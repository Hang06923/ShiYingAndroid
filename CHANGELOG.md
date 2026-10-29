# Changelog

## 1.0.5

- Restored the visible Android gesture handle while keeping it transparent and immersive.
- Added a status-bar blur layer whose intensity fades downward.

## 1.0.4

- Removed adaptive and round icon variants.
- The launcher now uses one original-ratio bitmap so the supplied icon is not re-composed, stretched, or padded by adaptive-icon templates.

## 1.0.3

- Hid the page scrollbar without changing the content layout.
- Made the Android navigation bar and gesture handle immersive; they remain temporarily available from the screen edge.
- Replaced the launcher icon with the supplied black and gold “拾影” icon.

## 1.0.2

- Fixed Douyin video files failing at the final MediaStore save step.
- Video files now use the Android Downloads collection while remaining under `Download/ShiYing`.
- Added detailed local logging and a specific storage error message for final-save failures.
- Allowed additional Douyin `ttwid` handshake retries when the share endpoint delays its data response.

## 1.0.1

- Fixed Douyin short-link IDs being read before redirect resolution completed.
- Added the `ttwid` share-page handshake required by the current Douyin endpoint.
- Added Douyin image-gallery parsing and local image export.
- Verified both user-provided Douyin links extract valid `video/mp4` media.
