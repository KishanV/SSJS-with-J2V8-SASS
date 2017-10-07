package com.spidren.builtin;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * Created by linedeer on 12/11/2016.
 */
public class jsTest implements JavaCallback {

    V8 v8;
    public jsTest(V8 v8) {
        this.v8 = v8;
    }

    public Object invoke(final V8Object receiver, final V8Array parameters) {
        //V8Object va = new V8Object(v8);
        return "ok ok";
    }

}
