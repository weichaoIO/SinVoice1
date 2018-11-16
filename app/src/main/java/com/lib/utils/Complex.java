package com.lib.utils;

public class Complex {
    private double a, b;//复数的实部和虚部

    public Complex(double a, double b) {
        this.a = a;
        this.b = b;
    }

    public void Change(double a, double b) {
        this.a = a;
        this.b = b;
    }

    public String toString() {
        if (b > 0.0) {
            if (a == 0.0)
                return b + "*i";
            else
                return a + "+" + b + "*i";
        } else if (b < 0.0) {
            if (a == 0.0)
                return b + "*i";
            else
                return a + "" + b + "*i";//注意
        } else
            return a + "";
    }

    public Complex plus(Complex Z) {
        double aa = this.a + Z.a;
        double bb = this.b + Z.b;
        return new Complex(aa, bb);
    }

    public Complex conjugate(){
        return new Complex(a, 0 - b);
    }

    public Complex minus(Complex Z) {
        double aa = this.a - Z.a;
        double bb = this.b - Z.b;
        return new Complex(aa, bb);
    }

    public Complex times(Complex Z) {
        double aa = this.a * Z.a - this.b * Z.b;
        double bb = this.b * Z.a + this.a * Z.b;
        return new Complex(aa, bb);
    }
    public Complex times(double x) {
        return new Complex(a * x, b * x);
    }

    public double abs(){
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }

    public Complex divide(Complex Z) {
        Z.Change(Z.a, -Z.b);
        Complex ZZ = this.times(Z);
        //System.out.println(ZZ.a+" "+ZZ.b);
        ZZ.a /= (Z.a * Z.a + Z.b * Z.b);
        ZZ.b /= (Z.a * Z.a + Z.b * Z.b);
        return ZZ;
    }


}