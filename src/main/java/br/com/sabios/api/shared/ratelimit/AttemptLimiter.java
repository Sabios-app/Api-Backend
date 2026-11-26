package br.com.sabios.api.shared.ratelimit;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de tentativas por chave com janela deslizante, em memória.
 *
 * <p>Limitação conhecida: o estado vive em uma única instância da aplicação. Com várias réplicas, cada uma
 * mantém a própria contagem (o limite efetivo vira N vezes maior); para isso seria necessário um armazenamento
 * compartilhado, como o Redis.
 */
public class AttemptLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final long windowNanos;
    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.windowNanos = window.toNanos();
    }

    /** Segundos até a chave poder tentar de novo; 0 se não estiver bloqueada. */
    public long retryAfterSeconds(String key) {
        Deque<Long> deque = attempts.get(key);
        if (deque == null) {
            return 0;
        }
        synchronized (deque) {
            long now = System.nanoTime();
            evict(deque, now);
            if (deque.size() < maxAttempts) {
                return 0;
            }
            long waitNanos = deque.peekFirst() + windowNanos - now;
            return Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
        }
    }

    /** Registra uma tentativa para a chave. */
    public void record(String key) {
        if (attempts.size() > CLEANUP_THRESHOLD) {
            purgeStale();
        }
        Deque<Long> deque = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            long now = System.nanoTime();
            evict(deque, now);
            deque.addLast(now);
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private void evict(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= windowNanos) {
            deque.pollFirst();
        }
    }

    private void purgeStale() {
        long now = System.nanoTime();
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                evict(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }
}
