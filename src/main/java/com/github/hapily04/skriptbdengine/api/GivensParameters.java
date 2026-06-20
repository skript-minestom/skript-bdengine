package com.github.hapily04.skriptbdengine.api;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

record GivensParameters(float sinHalf, float cosHalf) {

    static GivensParameters fromUnnormalized(float a, float b) {
        float invLength = Math.invsqrt(a * a + b * b);
        return new GivensParameters(invLength * a, invLength * b);
    }

    static GivensParameters fromPositiveAngle(float angle) {
        float sinHalf = Math.sin(angle / 2.0F);
        float cosHalf = Math.cosFromSin(sinHalf, angle / 2.0F);
        return new GivensParameters(sinHalf, cosHalf);
    }

    GivensParameters inverse() {
        return new GivensParameters(-sinHalf, cosHalf);
    }

    Quaternionf aroundX(Quaternionf dest) {
        return dest.set(sinHalf, 0.0F, 0.0F, cosHalf);
    }

    Quaternionf aroundY(Quaternionf dest) {
        return dest.set(0.0F, sinHalf, 0.0F, cosHalf);
    }

    Quaternionf aroundZ(Quaternionf dest) {
        return dest.set(0.0F, 0.0F, sinHalf, cosHalf);
    }

    float cos() {
        return cosHalf * cosHalf - sinHalf * sinHalf;
    }

    float sin() {
        return 2.0F * sinHalf * cosHalf;
    }

    Matrix3f aroundX(Matrix3f dest) {
        dest.m01 = 0.0F;
        dest.m02 = 0.0F;
        dest.m10 = 0.0F;
        dest.m20 = 0.0F;
        float c = cos();
        float s = sin();
        dest.m11 = c;
        dest.m22 = c;
        dest.m12 = s;
        dest.m21 = -s;
        dest.m00 = 1.0F;
        return dest;
    }

    Matrix3f aroundY(Matrix3f dest) {
        dest.m01 = 0.0F;
        dest.m10 = 0.0F;
        dest.m12 = 0.0F;
        dest.m21 = 0.0F;
        float c = cos();
        float s = sin();
        dest.m00 = c;
        dest.m22 = c;
        dest.m02 = -s;
        dest.m20 = s;
        dest.m11 = 1.0F;
        return dest;
    }

    Matrix3f aroundZ(Matrix3f dest) {
        dest.m02 = 0.0F;
        dest.m12 = 0.0F;
        dest.m20 = 0.0F;
        dest.m21 = 0.0F;
        float c = cos();
        float s = sin();
        dest.m00 = c;
        dest.m11 = c;
        dest.m01 = s;
        dest.m10 = -s;
        dest.m22 = 1.0F;
        return dest;
    }
}
