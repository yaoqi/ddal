/*
 * Copyright 2016-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hellojavaer.ddr.core.sharding;

/**
 *
 * @author <a href="mailto:hellojavaer@gmail.com">Kaiming Zou</a>,created on 20/12/2016.
 */
public class RouteInfo {

    private String scName;
    private String tbName;
    private String sdName;

    public RouteInfo() {
    }

    public RouteInfo(String scName, String tbName, String sdName) {
        this.scName = scName;
        this.tbName = tbName;
        this.sdName = sdName;
    }

    public String getScName() {
        return scName;
    }

    public void setScName(String scName) {
        this.scName = scName;
    }

    public String getTbName() {
        return tbName;
    }

    public void setTbName(String tbName) {
        this.tbName = tbName;
    }

    public String getSdName() {
        return sdName;
    }

    public void setSdName(String sdName) {
        this.sdName = sdName;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{scName:'" + scName + "', tbName:'" + tbName + "', sdName:'" + sdName + "'}").toString();
    }
}
