//package com.mapin.content.infrastructure;
//
//import com.mapin.content.port.EmbeddingClient;
//import com.mapin.content.port.VectorSearchResult;
//import com.mapin.content.port.VectorStoreClient;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class EmbeddingConfig {
//
//    @Bean
//    @ConditionalOnMissingBean(EmbeddingClient.class)
//    public EmbeddingClient simpleEmbeddingClient() {
//        return new SimpleEmbeddingClient();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(VectorStoreClient.class)
//    public VectorStoreClient inMemoryVectorStoreClient() {
//        return new InMemoryVectorStoreClient();
//    }
//
//    private static final class SimpleEmbeddingClient implements EmbeddingClient {
//
//        private static final String MODEL_NAME = "simple-embedding-model";
//
//        @Override
//        public List<Float> embed(String text) {
//            String safeText = text == null ? "" : text;
//            float[] buckets = new float[8];
//            for (int i = 0; i < safeText.length(); i++) {
//                char ch = safeText.charAt(i);
//                buckets[i % buckets.length] += ch;
//            }
//            List<Float> vector = new ArrayList<>(buckets.length);
//            float normalizer = safeText.isEmpty() ? 1f : safeText.length();
//            for (float bucket : buckets) {
//                vector.add(bucket / normalizer);
//            }
//            return vector;
//        }
//
//        @Override
//        public String modelName() {
//            return MODEL_NAME;
//        }
//    }
//
//    private static final class InMemoryVectorStoreClient implements VectorStoreClient {
//
//        private final Map<String, VectorRecord> store = new ConcurrentHashMap<>();
//
//        @Override
//        public void upsert(String id, List<Float> vector, Map<String, Object> metadata) {
//            store.put(id, new VectorRecord(vector, metadata));
//        }
//
//        @Override
//        public List<VectorSearchResult> searchById(String vectorId, int topK) {
//            VectorRecord source = store.get(vectorId);
//            if (source == null) {
//                throw new IllegalArgumentException("벡터를 찾을 수 없습니다. id=" + vectorId);
//            }
//
//            return store.entrySet().stream()
//                    .map(entry -> new VectorSearchResult(
//                            entry.getKey(),
//                            cosineSimilarity(source.vector(), entry.getValue().vector()),
//                            entry.getValue().metadata()
//                    ))
//                    .sorted((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()))
//                    .limit(topK)
//                    .toList();
//        }
//
//        private double cosineSimilarity(List<Float> a, List<Float> b) {
//            if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
//                return 0d;
//            }
//            int size = Math.min(a.size(), b.size());
//            double dot = 0d;
//            double magA = 0d;
//            double magB = 0d;
//            for (int i = 0; i < size; i++) {
//                double va = a.get(i);
//                double vb = b.get(i);
//                dot += va * vb;
//                magA += va * va;
//                magB += vb * vb;
//            }
//            if (magA == 0 || magB == 0) {
//                return 0d;
//            }
//            return dot / (Math.sqrt(magA) * Math.sqrt(magB));
//        }
//
//        private record VectorRecord(List<Float> vector, Map<String, Object> metadata) {
//        }
//    }
//}
