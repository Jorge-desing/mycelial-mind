package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.data.CodexBookData;
import com.alex.mycelialmind.data.CodexReloadListener;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks Codex entries newly unlocked during the current client session
 * so the Codex index and HUD can surface clean "NEW" badges until read.
 */
public final class ClientUnreadCodexTracker {
    private static final Map<String, Set<String>> UNREAD_BY_CHAPTER = new HashMap<>();

    private ClientUnreadCodexTracker() {
    }

    public static synchronized void clear() {
        UNREAD_BY_CHAPTER.clear();
    }

    public static synchronized void markUnread(@Nullable String chapterId, @Nullable String entryId) {
        if (chapterId == null || chapterId.isBlank() || entryId == null || entryId.isBlank()) {
            return;
        }
        UNREAD_BY_CHAPTER.computeIfAbsent(chapterId, ignored -> new HashSet<>()).add(entryId);
    }

    public static synchronized void markRead(@Nullable String chapterId, @Nullable String entryId) {
        if (chapterId == null || entryId == null) {
            return;
        }
        Set<String> set = UNREAD_BY_CHAPTER.get(chapterId);
        if (set != null) {
            set.remove(entryId);
            if (set.isEmpty()) {
                UNREAD_BY_CHAPTER.remove(chapterId);
            }
        }
    }

    public static synchronized boolean hasUnreadInChapter(@Nullable String chapterId) {
        if (chapterId == null) {
            return false;
        }
        Set<String> set = UNREAD_BY_CHAPTER.get(chapterId);
        return set != null && !set.isEmpty();
    }

    public static synchronized boolean isEntryUnread(@Nullable String chapterId, @Nullable String entryId) {
        if (chapterId == null || entryId == null) {
            return false;
        }
        Set<String> set = UNREAD_BY_CHAPTER.get(chapterId);
        return set != null && set.contains(entryId);
    }

    public static synchronized int getTotalUnreadCount() {
        int count = 0;
        for (Set<String> set : UNREAD_BY_CHAPTER.values()) {
            count += set.size();
        }
        return count;
    }

    public static void markActEntriesUnread(int actNumber) {
        if (actNumber < 2 || actNumber > 4) {
            return;
        }
        String gate = "act_" + actNumber;
        CodexBookData book = CodexReloadListener.getBook();
        if (book == null) {
            return;
        }
        for (CodexBookData.Chapter ch : book.chapters()) {
            for (CodexBookData.Entry entry : ch.entries()) {
                if (gate.equals(entry.unlockGate())) {
                    markUnread(ch.id(), entry.id());
                }
            }
        }
    }
}
