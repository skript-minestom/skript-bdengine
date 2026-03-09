package com.github.hapily04.skriptbdengine.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptbdengine.api.Animation;
import com.github.hapily04.skriptbdengine.api.AnimationModel;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

public class ExprModelAnimation extends SimplePropertyExpression<AnimationModel, String> {

    static {
        register(ExprModelAnimation.class, String.class, "[bd[engine][ ]]animation", "bdmodels");
    }

    @Override
    public String convert(AnimationModel animationModel) {
        return animationModel.getCurrentAnimation();
    }

    @Override
    public Class<?> [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, RESET, DELETE -> CollectionUtils.array(Animation.class);
            default -> null;
        };
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Animation animation = delta == null ? null : (Animation) delta[0];
        for (AnimationModel model : getExpr().getArray(event)) {
            if (mode != Changer.ChangeMode.SET) {
                model.clearAnimation();
                continue;
            }

            if (animation == null) return;
            String id = animation.id();
            boolean loop = animation.loop();
            model.play(id, loop);
        }

    }

    @Override
    protected String getPropertyName() {
        return "animation";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

}
