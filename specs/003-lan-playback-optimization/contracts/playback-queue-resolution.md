# Contract: Playback Queue Resolution and Hydration

**Branch**: `003-lan-playback-optimization` | **Date**: 2026-05-17

## Resolver contract

```kotlin
interface PlaybackQueueResolver {
    suspend fun resolve(context: PlaybackLaunchContext): PlaybackQueueSnapshot
}
```

## Precedence rules

1. `API_PLAYLIST`
2. `visibleSiblingUris` từ launcher
3. local indexed folder fallback theo sort hiện hành của app
4. single-item fallback

## Hydration contract

```kotlin
interface QueueHydrationCoordinator {
    fun startCurrentItemImmediately(context: PlaybackLaunchContext)
    suspend fun hydrateQueue(context: PlaybackLaunchContext): PlaybackQueueSnapshot
    fun applyQueue(snapshot: PlaybackQueueSnapshot)
}
```

## Behavioral rules

- `startCurrentItemImmediately()` phải cho phép video hiện tại `prepare()` trước khi full queue resolve xong.
- `hydrateQueue()` không được block thread UI.
- `applyQueue()` phải giữ nguyên current media item và current position nếu queue mới chỉ bổ sung sibling items.
- Queue cuối cùng không được chứa duplicate của media hiện tại.
- Nếu resolver không tìm được item hợp lệ theo hướng Next/Prev:
  - giữ nguyên media hiện tại
  - phát feedback ngắn cho người dùng

## Failure rules

- Nếu launcher không truyền ngữ cảnh thư mục và local fallback không resolve được:
  - queue trở thành `SINGLE_ITEM`
  - playback hiện tại vẫn tiếp tục
- Nếu hydrate queue thất bại sau khi video đã phát:
  - không được đóng player session
  - không được reset current item
  - chỉ hạ cấp khả năng Next/Prev theo thư mục cho phiên đó
