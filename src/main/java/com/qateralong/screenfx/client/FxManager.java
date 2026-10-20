package com.qateralong.screenfx.client;

import com.qateralong.screenfx.FxPayload;
import com.qateralong.screenfx.FxType;
import java.util.ArrayList;
import java.util.List;

public final class FxManager {
    private static final List<ActiveFx> ACTIVE = new ArrayList<>();
    private static List<ActiveFx> frame = List.of();

    private FxManager() {
    }

    public static synchronized void handle(FxPayload payload) {
        if (payload.effect().equals(FxPayload.CLEAR)) {
            for (ActiveFx fx : ACTIVE) {
                if (fx.matches(payload.key())) {
                    fx.release();
                }
            }
            return;
        }
        FxType type = FxType.byId(payload.effect());
        if (type == null) {
            return;
        }
        ActiveFx added = new ActiveFx(type, payload);
        for (ActiveFx fx : ACTIVE) {
            if (fx.key.equals(added.key)) {
                fx.release();
            }
        }
        ACTIVE.add(added);
    }

    public static synchronized List<ActiveFx> snapshot() {
        ACTIVE.removeIf(ActiveFx::finished);
        frame = List.copyOf(ACTIVE);
        return frame;
    }

    public static List<ActiveFx> current() {
        return frame;
    }

    public static synchronized void clearAll() {
        ACTIVE.clear();
        frame = List.of();
    }

    public static float strength(FxType type) {
        float total = 0.0F;
        for (ActiveFx fx : current()) {
            if (fx.type == type) {
                total = Math.max(total, fx.strength());
            }
        }
        return total;
    }
}
