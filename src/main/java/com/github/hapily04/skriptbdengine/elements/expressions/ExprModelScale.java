package com.github.hapily04.skriptbdengine.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import com.github.hapily04.skriptbdengine.api.AnimationModel;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;

@Name("Model Scale")
@Description("Gets or sets the uniform scale of a BDEngine model. Setting scale multiplies each part's translation and scale linearly, and animation keyframes respect the same factor.")
@Examples("""
    command /model <model-id: string>:
        trigger:
            set {_model} to new model from {_model-id}
            teleport {_model} to player in player's instance
            set scale of {_model} to 2
            set {_s} to scale of {_model}""")
@Since("1.0.0")
public class ExprModelScale extends SimplePropertyExpression<AnimationModel, Number> {

    static {
        register(ExprModelScale.class, Number.class, "model scale", "bdmodels");
    }

    @Override
    public Number convert(AnimationModel animationModel) {
        return animationModel.getModelScale();
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE, RESET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Number value = delta == null ? null : (Number) delta[0];
        for (AnimationModel model : getExpr().getArray(event)) {
            switch (mode) {
                case SET -> {
                    if (value != null) model.setModelScale(value.floatValue());
                }
                case ADD -> {
                    if (value != null) model.setModelScale(model.getModelScale() + value.floatValue());
                }
                case REMOVE -> {
                    if (value != null) model.setModelScale(model.getModelScale() - value.floatValue());
                }
                case RESET -> model.setModelScale(1f);
                default -> {}
            }
        }
    }

    @Override
    protected String getPropertyName() {
        return "scale";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

}
