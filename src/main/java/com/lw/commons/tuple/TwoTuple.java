package com.lw.commons.tuple;

/***
 *                    .::::.
 *                  .::::::::.
 *                 :::::::::::  
 *             ..:::::::::::'
 *           '::::::::::::'
 *             .::::::::::
 *        '::::::::::::::..
 *             ..::::::::::::.
 *           ``::::::::::::::::
 *            ::::``:::::::::'        .:::.
 *           ::::'   ':::::'       .::::::::.
 *         .::::'      ::::     .:::::::'::::.
 *        .:::'       :::::  .:::::::::' ':::::.
 *       .::'        :::::.:::::::::'      ':::::.
 *      .::'         ::::::::::::::'         ``::::.
 *  ...:::           ::::::::::::'              ``::.
 * ```` ':.          ':::::::::'                  ::::..
 *                    '.:::::'                    ':'````..
 *  @data 2020/10/16 10:21
 *  @author lw
 */
public class TwoTuple<A, B> {
    public final A first;
    public final B second;

    public A getFirst() {
        return this.first;
    }

    public B getSecond() {
        return this.second;
    }

    public TwoTuple(A a, B b) {
        this.first = a;
        this.second = b;
    }

    @Override
    public String toString() {
        return "(" + this.first + ", " + this.second + ")";
    }

    @Override
    public int hashCode() {
        int prime = 1;
        int result = 1;
        result = 31 * result + (this.first == null ? 0 : this.first.hashCode());
        result = 31 * result + (this.second == null ? 0 : this.second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            TwoTuple other = (TwoTuple)obj;
            if (this.first == null) {
                if (other.first != null) {
                    return false;
                }
            } else if (!this.first.equals(other.first)) {
                return false;
            }

            if (this.second == null) {
                if (other.second != null) {
                    return false;
                }
            } else if (!this.second.equals(other.second)) {
                return false;
            }

            return true;
        }
    }
}
