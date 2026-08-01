package com.github.hapily04.skriptbdengine.registration;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.function.*;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.yggdrasil.Fields;
import com.github.hapily04.skriptbdengine.api.Animation;
import com.github.hapily04.skriptbdengine.api.AnimationModel;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;

public class Registration {

    static {
        // Classes
        Classes.registerClass(new ClassInfo<>(Animation.class, "bdanimation")
            .user("bd ?animations?")
            .name("BDEngine Animation")
            .description("An animation for a BDEngine model that contains an id and whether it loops or not.")
            .examples("set bdengine animation of {_a} to bdAnimation(\"walk\", true)")
            .parser(new Parser<>() {
                @Override
                public boolean canParse(@NotNull ParseContext context) {
                    return false;
                }

                @Override
                public @NotNull String toString(@NotNull Animation o, int flags) {
                    return toVariableNameString(o);
                }

                @Override
                public @NotNull String toVariableNameString(@NotNull Animation o) {
                    return "bdengine animation with id: " + o.id() + " looping: " + o.loop();
                }
            })
            .serializer(new Serializer<>() {
                @Override
                public @NotNull Fields serialize(@NotNull Animation o) throws NotSerializableException {
                    Fields fields = new Fields();
                    fields.putObject("id", o.id());
                    fields.putPrimitive("loop", o.loop());
                    return fields;
                }

                @Override
                public void deserialize(@NotNull Animation o, @NotNull Fields f) {
                    assert false;
                }

                @Override
                protected @NotNull Animation deserialize(@NotNull Fields f) throws StreamCorruptedException {
                    String id = f.getObject("id", String.class);
                    boolean loop = f.getPrimitive("loop", boolean.class);
                    return new Animation(id, loop);
                }

                @Override
                public boolean mustSyncDeserialization() {
                    return false;
                }

                @Override
                protected boolean canBeInstantiated() {
                    return false;
                }
            }));
        Classes.registerClass(new ClassInfo<>(AnimationModel.class, "bdmodel")
            .user("bd ?models?")
            .name("BDEngine Model")
            .description("A model from BDEngine consisting of a marker entity and its model entities.")
            .defaultExpression(new EventValueExpression<>(AnimationModel.class)));

        // Functions
        Functions.registerFunction(new JavaFunction<>("bdAnimation", new Parameter[]{
            new Parameter<>("id", DefaultClasses.STRING, true, null),
            new Parameter<>("loop", DefaultClasses.BOOLEAN, true, null)
        }, Classes.getExactClassInfo(Animation.class), true) {
            @Override
            public @Nullable Animation[] execute(FunctionEvent<?> e, Object[][] params) {
                if (parametersNull(params, 1)) return new Animation[0];
                String id = (String) params[0][0];
                boolean loop = (boolean) params[1][0];
                return new Animation[]{new Animation(id, loop)};
            }
        });

        // Converters
        Converters.registerConverter(Entity.class, AnimationModel.class, entity -> {
            if (entity instanceof AnimationModel model) return model;
            return null;
        });
    }

    private static boolean parametersNull(Object[][] params, int toIndex) {
        for (int i = 0; i <= toIndex; i++) {
            if (params[i].length == 0) return true;
        }
        return false;
    }

}
