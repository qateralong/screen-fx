package com.qateralong.screenfx.client;

import com.qateralong.screenfx.FxPayload;
import com.qateralong.screenfx.FxType;
import java.util.ArrayList;
import java.util.List;

public final class FxManager {
    private static final List<ActiveFx> ACTIVE = new ArrayList<>();

    private FxManager() {
    }

    public static synchronized void handle(FxPayload payload) {
        if (payload.effect().startsWith(FxPayload.CLEAR)) {
            String only = payload.effect().length() > FxPayload.CLEAR.length() + 1 ? payload.effect().substring(FxPayload.CLEAR.length() + 1) : "";
            for (ActiveFx fx : ACTIVE) {
                if (only.isEmpty() || fx.type.id().equals(only)) {
                    fx.release();
                }
            }
            return;
        }
        FxType type = FxType.byId(payload.effect());
        if (type == null) {
            return;
        }
        for (ActiveFx fx : ACTIVE) {
            if (fx.type == type) {
                fx.release();
            }
        }
        ACTIVE.add(new ActiveFx(type, payload.durationMs(), payload.fadeMs(), payload.intensity(), payload.color()));
    }

    public static synchronized List<ActiveFx> snapshot() {
        ACTIVE.removeIf(ActiveFx::finished);
        return new ArrayList<>(ACTIVE);
    }

    public static synchronized void clearAll() {
        ACTIVE.clear();
    }

    public static float strength(FxType type) {
        float total = 0.0F;
        for (ActiveFx fx : snapshot()) {
            if (fx.type == type) {
                total = Math.max(total, fx.strength());
            }
        }
        return total;
    }
}
