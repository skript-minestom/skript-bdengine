package com.github.hapily04.skriptbdengine.api;

import com.google.gson.JsonArray;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class DisplayMatrixUtil {

    private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
    private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle(0.7853982F);

    private DisplayMatrixUtil() {}

    record CachedTransform(float[] translation, float[] leftRotation, float[] scale, float[] rightRotation) {
        void applyTo(AbstractDisplayMeta meta) {
            applyTo(meta, 1f);
        }

        void applyTo(AbstractDisplayMeta meta, float modelScale) {
            meta.setTranslation(new Vec(
                translation[0] * modelScale,
                translation[1] * modelScale,
                translation[2] * modelScale
            ));
            meta.setScale(new Vec(
                scale[0] * modelScale,
                scale[1] * modelScale,
                scale[2] * modelScale
            ));
            meta.setLeftRotation(leftRotation);
            meta.setRightRotation(rightRotation);
        }
    }

    static CachedTransform cacheFromFlat(float[] values) {
        Matrix4f matrix = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            matrix.set(i / 4, i % 4, values[i]);
        }
        matrix.transpose();
        return cacheFromMatrix(matrix);
    }

    static CachedTransform cacheFromJsonArray(JsonArray values) {
        Matrix4f matrix = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            matrix.set(i / 4, i % 4, values.get(i).getAsFloat());
        }
        matrix.transpose();
        return cacheFromMatrix(matrix);
    }

    private static CachedTransform cacheFromMatrix(Matrix4f matrix) {
        DecomposedTransform transform = decompose(matrix);
        return new CachedTransform(
            new float[]{transform.translation().x, transform.translation().y, transform.translation().z},
            new float[]{transform.leftRotation().x, transform.leftRotation().y, transform.leftRotation().z, transform.leftRotation().w},
            new float[]{transform.scale().x, transform.scale().y, transform.scale().z},
            new float[]{transform.rightRotation().x, transform.rightRotation().y, transform.rightRotation().z, transform.rightRotation().w}
        );
    }

    record DecomposedTransform(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {}

    static DecomposedTransform decompose(Matrix4f matrix) {
        float invW = Math.abs(matrix.m33()) < 1.0E-6F ? 1.0F : 1.0F / matrix.m33();
        Vector3f translation = matrix.getTranslation(new Vector3f()).mul(invW);
        SvdResult svd = svdDecompose(new Matrix3f(matrix).scale(invW));
        return new DecomposedTransform(translation, svd.left(), svd.scale(), svd.right());
    }

    private record SvdResult(Quaternionf left, Vector3f scale, Quaternionf right) {}

    private static GivensParameters approxGivensQuat(float a, float b, float c) {
        float t = 2.0F * (a - c);
        return G * b * b < t * t ? GivensParameters.fromUnnormalized(b, t) : PI_4;
    }

    private static GivensParameters qrGivensQuat(float a, float b) {
        float hypot = (float) java.lang.Math.hypot(a, b);
        float sin = hypot > 1.0E-6F ? b : 0.0F;
        float cos = Math.abs(a) + Math.max(hypot, 1.0E-6F);
        if (a < 0.0F) {
            float tmp = sin;
            sin = cos;
            cos = tmp;
        }
        return GivensParameters.fromUnnormalized(sin, cos);
    }

    private static void similarityTransform(Matrix3f matrix, Matrix3f scratch) {
        matrix.mul(scratch);
        scratch.transpose();
        scratch.mul(matrix);
        matrix.set(scratch);
    }

    private static void stepJacobi(Matrix3f matrix, Matrix3f scratch, Quaternionf step, Quaternionf accumulated) {
        GivensParameters givens;
        Quaternionf rotation;

        if (matrix.m01 * matrix.m01 + matrix.m10 * matrix.m10 > 1.0E-6F) {
            givens = approxGivensQuat(matrix.m00, 0.5F * (matrix.m01 + matrix.m10), matrix.m11);
            rotation = givens.aroundZ(step);
            accumulated.mul(rotation);
            givens.aroundZ(scratch);
            similarityTransform(matrix, scratch);
        }

        if (matrix.m02 * matrix.m02 + matrix.m20 * matrix.m20 > 1.0E-6F) {
            givens = approxGivensQuat(matrix.m00, 0.5F * (matrix.m02 + matrix.m20), matrix.m22).inverse();
            rotation = givens.aroundY(step);
            accumulated.mul(rotation);
            givens.aroundY(scratch);
            similarityTransform(matrix, scratch);
        }

        if (matrix.m12 * matrix.m12 + matrix.m21 * matrix.m21 > 1.0E-6F) {
            givens = approxGivensQuat(matrix.m11, 0.5F * (matrix.m12 + matrix.m21), matrix.m22);
            rotation = givens.aroundX(step);
            accumulated.mul(rotation);
            givens.aroundX(scratch);
            similarityTransform(matrix, scratch);
        }
    }

    private static Quaternionf eigenvalueJacobi(Matrix3f matrix, int iterations) {
        Quaternionf accumulated = new Quaternionf();
        Matrix3f scratch = new Matrix3f();
        Quaternionf step = new Quaternionf();
        for (int i = 0; i < iterations; i++) {
            stepJacobi(matrix, scratch, step, accumulated);
        }
        accumulated.normalize();
        return accumulated;
    }

    private static SvdResult svdDecompose(Matrix3f matrix3f) {
        Matrix3f ata = new Matrix3f(matrix3f);
        ata.transpose();
        ata.mul(matrix3f);
        Quaternionf rightRotation = eigenvalueJacobi(ata, 5);
        float a = ata.m00;
        float b = ata.m11;
        boolean aIsZero = (double) a < 1.0E-6D;
        boolean bIsZero = (double) b < 1.0E-6D;

        Matrix3f rotated = matrix3f.rotate(rightRotation);
        Quaternionf leftRotation = new Quaternionf();
        Quaternionf step = new Quaternionf();
        Matrix3f scratch = new Matrix3f();
        GivensParameters givens;

        if (aIsZero) {
            givens = qrGivensQuat(rotated.m11, -rotated.m10);
        } else {
            givens = qrGivensQuat(rotated.m00, rotated.m01);
        }

        Quaternionf zStep = givens.aroundZ(step);
        Matrix3f stage = givens.aroundZ(scratch);
        leftRotation.mul(zStep);
        stage.transpose().mul(rotated);

        if (aIsZero) {
            givens = qrGivensQuat(stage.m22, -stage.m20);
        } else {
            givens = qrGivensQuat(stage.m00, stage.m02);
        }

        givens = givens.inverse();
        Quaternionf yStep = givens.aroundY(step);
        Matrix3f yMatrix = givens.aroundY(rotated);
        leftRotation.mul(yStep);
        yMatrix.transpose().mul(stage);

        if (bIsZero) {
            givens = qrGivensQuat(yMatrix.m22, -yMatrix.m21);
        } else {
            givens = qrGivensQuat(yMatrix.m11, yMatrix.m12);
        }

        Quaternionf xStep = givens.aroundX(step);
        Matrix3f xMatrix = givens.aroundX(scratch);
        leftRotation.mul(xStep);
        xMatrix.transpose().mul(yMatrix);

        Vector3f scale = new Vector3f(xMatrix.m00, xMatrix.m11, xMatrix.m22);
        return new SvdResult(leftRotation, scale, rightRotation.conjugate(new Quaternionf()));
    }
}
