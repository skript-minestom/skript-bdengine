package com.github.hapily04.skriptbdengine.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptbdengine.api.Animation;
import com.github.hapily04.skriptbdengine.api.AnimationModel;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Model Animation")
@Description("Allows you to set the active animation of the provided model.")
@Examples("""
    command /model <model-id: string>:
        trigger:
            set {_model} to new model from {_model-id} # Converts the model to a json file internally and builds an entity from that
            teleport {_model} to player in player's instance # spawns in the model
            set animation of {_model} to bdAnimation("default", true) # run the default animation and have it loop""")
@Since("1.0.0")
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
