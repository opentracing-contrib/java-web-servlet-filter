/*
 * Copyright 2016-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.web.servlet.filter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class MultivaluedMapFlatIteratorTest {

    @Test
    public void testEmpty() {
        Map<String, List<String>> map = new HashMap<>();

        HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<String, String> iterator = new
                HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<>(map.entrySet());

        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testEmptyValue() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key", Collections.<String>emptyList());

        HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<String, String> iterator = new
                HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<>(map.entrySet());

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", null), iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testOneKeyOneValue() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key", new ArrayList<>(Arrays.asList("value")));

        HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<String, String> iterator = new
                HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<>(map.entrySet());

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", "value"), iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testOneKeyMultipleValues() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key", new ArrayList<>(Arrays.asList("value1", "value2")));

        HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<String, String> iterator = new
                HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<>(map.entrySet());

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", "value1"), iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", "value2"), iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void testMultipleKeysMultipleValues() {
        Map<String, List<String>> map = new TreeMap<>();
        map.put("key", new ArrayList<>(Arrays.asList("value1", "value2")));
        map.put("key2", new ArrayList<>(Arrays.asList("value1", "value2")));

        HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<String, String> iterator = new
                HttpServletRequestExtractAdapter.MultivaluedMapFlatIterator<>(map.entrySet());

        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", "value1"), iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key", "value2"), iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key2", "value1"), iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(new AbstractMap.SimpleImmutableEntry<>("key2", "value2"), iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }
}
