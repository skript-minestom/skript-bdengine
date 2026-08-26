package com.github.hapily04.skriptbdengine.api;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.MetadataHolder;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;

public class AnimationModelMeta extends AbstractDisplayMeta {

    private final AnimationModel animationModel;

    AnimationModelMeta(AnimationModel entity, MetadataHolder metadata) {
        super(entity, metadata);
        this.animationModel = entity;
    }

    private void forEachDisplayMeta(Consumer<AbstractDisplayMeta> action) {
        for (var entity : animationModel.entities) {
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                action.accept(displayMeta);
            }
        }
    }

    private AbstractDisplayMeta firstDisplayMeta() {
        for (var entity : animationModel.entities) {
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                return displayMeta;
            }
        }
        return null;
    }

    @Override
    public int getTransformationInterpolationStartDelta() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getTransformationInterpolationStartDelta() : super.getTransformationInterpolationStartDelta();
    }

    @Override
    public void setTransformationInterpolationStartDelta(int value) {
        forEachDisplayMeta(meta -> meta.setTransformationInterpolationStartDelta(value));
    }

    @Override
    public int getTransformationInterpolationDuration() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getTransformationInterpolationDuration() : super.getTransformationInterpolationDuration();
    }

    @Override
    public void setTransformationInterpolationDuration(int value) {
        forEachDisplayMeta(meta -> meta.setTransformationInterpolationDuration(value));
    }

    @Override
    public int getPosRotInterpolationDuration() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getPosRotInterpolationDuration() : super.getPosRotInterpolationDuration();
    }

    @Override
    public void setPosRotInterpolationDuration(int value) {
        forEachDisplayMeta(meta -> meta.setPosRotInterpolationDuration(value));
    }

    @Override
    public @NonNull Point getTranslation() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getTranslation() : super.getTranslation();
    }

    @Override
    public void setTranslation(@NonNull Point value) {
        forEachDisplayMeta(meta -> meta.setTranslation(value));
    }

    @Override
    public @NonNull Vec getScale() {
        float scale = animationModel.getModelScale();
        return new Vec(scale, scale, scale);
    }

    @Override
    public void setScale(@NonNull Vec value) {
        animationModel.setModelScale((float) value.x());
    }

    @Override
    public float @NonNull [] getLeftRotation() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getLeftRotation() : super.getLeftRotation();
    }

    @Override
    public void setLeftRotation(float @NonNull [] value) {
        forEachDisplayMeta(meta -> meta.setLeftRotation(value));
    }

    @Override
    public float @NonNull [] getRightRotation() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getRightRotation() : super.getRightRotation();
    }

    @Override
    public void setRightRotation(float[] value) {
        forEachDisplayMeta(meta -> meta.setRightRotation(value));
    }

    @Override
    public @NonNull BillboardConstraints getBillboardRenderConstraints() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getBillboardRenderConstraints() : super.getBillboardRenderConstraints();
    }

    @Override
    public void setBillboardRenderConstraints(@NonNull BillboardConstraints value) {
        forEachDisplayMeta(meta -> meta.setBillboardRenderConstraints(value));
    }

    @Override
    public int getBrightnessOverride() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getBrightnessOverride() : super.getBrightnessOverride();
    }

    @Override
    public void setBrightnessOverride(int value) {
        forEachDisplayMeta(meta -> meta.setBrightnessOverride(value));
    }

    @Override
    public void setBrightness(int blockLight, int skyLight) {
        forEachDisplayMeta(meta -> meta.setBrightness(blockLight, skyLight));
    }

    @Override
    public int getBlockLight() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getBlockLight() : super.getBlockLight();
    }

    @Override
    public int getSkyLight() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getSkyLight() : super.getSkyLight();
    }

    @Override
    public float getViewRange() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getViewRange() : super.getViewRange();
    }

    @Override
    public void setViewRange(float value) {
        forEachDisplayMeta(meta -> meta.setViewRange(value));
    }

    @Override
    public float getShadowRadius() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getShadowRadius() : super.getShadowRadius();
    }

    @Override
    public void setShadowRadius(float value) {
        forEachDisplayMeta(meta -> meta.setShadowRadius(value));
    }

    @Override
    public float getShadowStrength() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getShadowStrength() : super.getShadowStrength();
    }

    @Override
    public void setShadowStrength(float value) {
        forEachDisplayMeta(meta -> meta.setShadowStrength(value));
    }

    @Override
    public float getWidth() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getWidth() : super.getWidth();
    }

    @Override
    public void setWidth(float value) {
        forEachDisplayMeta(meta -> meta.setWidth(value));
    }

    @Override
    public float getHeight() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getHeight() : super.getHeight();
    }

    @Override
    public void setHeight(float value) {
        forEachDisplayMeta(meta -> meta.setHeight(value));
    }

    @Override
    public int getGlowColorOverride() {
        AbstractDisplayMeta meta = firstDisplayMeta();
        return meta != null ? meta.getGlowColorOverride() : super.getGlowColorOverride();
    }

    @Override
    public void setGlowColorOverride(int value) {
        forEachDisplayMeta(meta -> meta.setGlowColorOverride(value));
    }

}
