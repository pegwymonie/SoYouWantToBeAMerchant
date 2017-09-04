/*
 * MIT License
 *
 * Copyright (c)  2017 pegwymonie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.potatohed.sywtb.merchant.util;

import java.util.Objects;

public class TariffDetails {
    Long regularTariff;
    Long demandTariff;

    public TariffDetails(Long regularTariff, Long demandTariff) {
        this.regularTariff = regularTariff;
        this.demandTariff = demandTariff;
    }

    public Long getRegularTariff() {
        return regularTariff;
    }

    public void setRegularTariff(Long regularTariff) {
        this.regularTariff = regularTariff;
    }

    public Long getDemandTariff() {
        return demandTariff;
    }

    public void setDemandTariff(Long demandTariff) {
        this.demandTariff = demandTariff;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TariffDetails that = (TariffDetails) o;
        return Objects.equals(regularTariff, that.regularTariff) &&
                Objects.equals(demandTariff, that.demandTariff);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regularTariff, demandTariff);
    }
}
