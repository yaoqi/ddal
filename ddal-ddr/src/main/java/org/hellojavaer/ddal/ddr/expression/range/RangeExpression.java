/*
 * Copyright 2016-2017 the original author or authors.
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
package org.hellojavaer.ddal.ddr.expression.range;

/**
 * 
 * 
 * @author <a href="mailto:hellojavaer@gmail.com">Kaiming Zou</a>,created on 24/11/2016.
 */
public class RangeExpression {

    private static final String NULL_STRING = new String("null");

    public static void parse(String str, RangeItemVisitor itemVisitor) {
        for (int startIndex = 0; startIndex <= str.length();) {
            startIndex = parse(str, null, startIndex, itemVisitor);
            if (startIndex == -1) {
                break;
            }
            startIndex++;
        }
    }

    private static int parse(String str, Object prefix, int startIndex, RangeItemVisitor itemVisitor) {
        if (startIndex >= str.length()) {
            if (NULL_STRING == prefix) {
                itemVisitor.visit(null);
            } else {
                itemVisitor.visit(prefix);
            }
            return startIndex;
        }
        boolean escape = false;
        StringBuilder sb = null;
        if (prefix != null) {
            sb = new StringBuilder();
            sb.append(prefix);
        }
        for (int index = startIndex;; index++) {
            if (index >= str.length()) {
                if (escape) {
                    throw new RangeExpressionException(str, str.length(), (char) 0,
                                                       "at outer statement block, only character '\\', ',' , '[' and ']' can be escaped");
                }
                if (sb != null) {
                    itemVisitor.visit(sb.toString());
                } else {
                    itemVisitor.visit(str.substring(startIndex, index));
                }
                return index;
            }
            char ch = str.charAt(index);
            if (escape) {// 语句块外特殊字符
                if (ch == '[' || ch == ']' || ch == ',' || ch == '\\') {//
                    sb.append(ch);
                    escape = false;
                } else {
                    throw new RangeExpressionException(str, index, ch,
                                                       "at outer statement block, only character '\\', ',' , '[' and ']' can be escaped");
                }
            } else {//
                if (ch == ',') {// 递归终结符
                    if (sb != null) {
                        itemVisitor.visit(sb.toString());
                    } else {
                        itemVisitor.visit(str.substring(startIndex, index));
                    }
                    return index;
                } else if (ch == '\\') {// 转义后使用sb做缓存,否则直接截取str子串
                    if (sb == null) {
                        sb = new StringBuilder();
                        sb.append(str.substring(startIndex, index));
                    }
                    escape = true;
                } else if (ch == '[') {// 区间开始符号 \\ 特殊字符 , [ ] \ . \s
                    return range(str, startIndex, itemVisitor, sb, index);
                } else if (ch == ']') {
                    throw new RangeExpressionException(str, index, ch, "expect closed expression. eg: [0,0..99]'");
                } else {// 普通字符
                    if (sb != null) {
                        sb.append(ch);
                    } else {
                        continue;
                    }
                }
            }
        }// for
    }

    private static int range(String str, int startIndex, RangeItemVisitor itemVisitor, StringBuilder sb, int index) {
        // 获取区域结束符位置
        int nextStart = -1;
        boolean escape0 = false;
        for (int k = index + 1; k < str.length(); k++) {
            char ch0 = str.charAt(k);
            if (escape0) {
                escape0 = false;
            } else if (ch0 == '\\') {
                escape0 = true;
            } else if (ch0 == ']') {
                nextStart = k + 1;
                break;
            }
        }
        if (nextStart == -1) {
            throw new RangeExpressionException(str, str.length(), (char) 0, "expect closed expression. eg: [0,0..99]'");
        }
        // 获取前缀
        String rangPrefix = null;
        if (sb != null) {
            rangPrefix = sb.toString();
        } else {
            rangPrefix = str.substring(startIndex, index);
        }
        int elePos = index + 1;// 最小描述符开始位置
        //
        int statusOfStart = 0;// 标识开始字符的状态 0:初始,1:数字,2:小写,3:多个小写,4:大写,5:多个大小,6:混合,7:正负号
        int statusOfEnd = 0;// 标识结束字符的状态
        int statusOfTemp = 0;
        boolean range = false;
        boolean escape1 = false;
        Object rangStart = null;
        StringBuilder sb1 = null;
        int xpos = 0;// .. 位置
        for (int i = index + 1;; i++) {
            if (i >= str.length()) {
                throw new RangeExpressionException(str, i, (char) 0, "expect closed expression. eg: [0,0..99]'");
            }
            char ch1 = str.charAt(i);
            if (escape1) {
                if (ch1 == '\\' || ch1 == '[' || ch1 == ']' || ch1 == ',' || ch1 == '.') {
                    sb1.append(ch1);
                } else if (ch1 == 's') {
                    sb1.append(' ');
                } else {
                    throw new RangeExpressionException(str, index, ch1,
                                                       "at inner statement block, only character '\\', ',' , '[', ']', '.' and 's' can be escaped");
                }
                escape1 = false;
                continue;
            }
            if (range) {
                statusOfTemp = statusOfEnd;
            } else {
                statusOfTemp = statusOfStart;
            }
            if (ch1 == '\\') {// 转义 key_word
                if (sb1 == null) {
                    sb1 = new StringBuilder();
                    sb1.append(str.substring(elePos, i));
                }
                escape1 = true;
            } else if (ch1 == '[') {// key_word
                throw new RangeExpressionException(str, index, ch1, "character '[' should be escaped");
            } else if (ch1 == '+' || ch1 == '-') {
                if (statusOfTemp == 0) {
                    statusOfTemp = 7;
                } else {
                    statusOfTemp = 6;
                }
            } else if (ch1 >= '0' && ch1 <= '9') {// 数字
                if (statusOfTemp == 0 || statusOfTemp == 7) {
                    statusOfTemp = 1;
                } else if (statusOfTemp != 1) {
                    statusOfTemp = 6;
                }
            } else if (ch1 == '.' && str.charAt(i + 1) == '.') {// support 1,2,4 key_word
                i++;
                if (range) {
                    throw new RangeExpressionException(str, i, ch1, ']');
                }
                if (statusOfTemp == 1) {
                    rangStart = Integer.parseInt(str.substring(elePos, i - 1));
                } else if (statusOfTemp == 2 || statusOfTemp == 4) {
                    rangStart = str.charAt(i - 1);
                } else {// ..
                    throw new RangeExpressionException(str, i, ch1, "expect closed expression. eg: [0,0..99]'");
                }
                xpos = i;
                range = true;
                continue;//
            } else if (ch1 == ',' || ch1 == ']') {// 结束符 key_word
                if (range && xpos + 1 == i) {
                    throw new RangeExpressionException(str, i, ch1,
                                                       "start expression and end expression don't match. eg: [089,0..99]'");
                }
                int epos = 0;// 返回下一个开始位置
                if (statusOfEnd != 0) {// ..
                    if (statusOfStart != statusOfEnd) {
                        throw new RangeExpressionException(str, i, ch1,
                                                           "start expression and end expression don't match. eg: [089,0..99]'");
                    } else {// 区间表达式
                        int s = ((Integer) rangStart).intValue();
                        int e = Integer.parseInt(str.substring(xpos + 1, i));
                        if (s <= e) {
                            for (int k = s; k <= e; k++) {
                                if (rangPrefix == null || rangPrefix.length() == 0) {
                                    epos = parse(str, k, nextStart, itemVisitor);
                                } else {
                                    epos = parse(str, rangPrefix + k, nextStart, itemVisitor);
                                }
                            }
                        } else {
                            for (int k = s; k >= e; k--) {
                                if (rangPrefix == null || rangPrefix.length() == 0) {
                                    epos = parse(str, k, nextStart, itemVisitor);
                                } else {
                                    epos = parse(str, rangPrefix + k, nextStart, itemVisitor);
                                }
                            }
                        }
                    }
                } else {// 单个表达式:可以是一个数字或字符,例如:[1,23,'a','bc',"d",'']
                    String singleRange = null;//
                    if (sb1 != null) {
                        singleRange = sb1.toString();
                    } else {
                        singleRange = str.substring(elePos, i);
                    }
                    if (singleRange.length() >= 2) {// 字符串至少需要2个字符
                        char endChar = singleRange.charAt(singleRange.length() - 1);
                        if (singleRange.charAt(0) == '\'') {
                            if (endChar != '\'') {
                                throw new RangeExpressionException(str, i, endChar, '\'');
                            } else {
                                epos = parse(str, rangPrefix + singleRange.substring(1, singleRange.length() - 1),
                                             nextStart, itemVisitor);
                            }
                        } else if (singleRange.charAt(0) == '\"') {
                            if (endChar != '\"') {
                                throw new RangeExpressionException(str, i, endChar, '\"');
                            } else {
                                epos = parse(str, rangPrefix + singleRange.substring(1, singleRange.length() - 1),
                                             nextStart, itemVisitor);
                            }
                        } else {
                            if (rangPrefix == null || rangPrefix.length() == 0) {
                                epos = parse(str, Integer.parseInt(singleRange), nextStart, itemVisitor);
                            } else {
                                epos = parse(str, rangPrefix + singleRange, nextStart, itemVisitor);
                            }
                        }
                    } else {
                        if (singleRange.length() == 0) {
                            if (ch1 == ']' && i == index + 1) {
                                return -1;
                            } else {
                                if (rangPrefix == null || rangPrefix.length() == 0) {
                                    epos = parse(str, NULL_STRING, nextStart, itemVisitor);
                                } else {
                                    epos = parse(str, rangPrefix + "null", nextStart, itemVisitor);
                                }
                            }
                        } else {
                            if (rangPrefix == null || rangPrefix.length() == 0) {
                                epos = parse(str, Integer.parseInt(singleRange), nextStart, itemVisitor);
                            } else {
                                epos = parse(str, rangPrefix + singleRange, nextStart, itemVisitor);
                            }
                        }
                    }
                }
                // 重置标识位
                range = false;
                statusOfTemp = 0;
                statusOfStart = 0;
                statusOfEnd = 0;
                sb1 = null;
                if (ch1 == ']') {// return
                    return epos;
                } else {
                    elePos = i + 1;
                }
            } else {
                statusOfStart = 6;
                if (sb1 != null) {
                    sb1.append(ch1);
                }
            }
            if (range) {
                statusOfEnd = statusOfTemp;
            } else {
                statusOfStart = statusOfTemp;
            }
        }
    }

}
