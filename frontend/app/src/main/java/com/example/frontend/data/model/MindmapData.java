package com.example.frontend.data.model;

import java.io.Serializable;
import java.util.List;

public class MindmapData implements Serializable {
    private String topic;
    private String summary;
    private List<Node> nodes;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    // --- Inner Class cho Ý Chính (Node) ---
    public static class Node implements Serializable {
        private String title;
        private String details;
        private List<SubNode> subNodes;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }

        public List<SubNode> getSubNodes() {
            return subNodes;
        }

        public void setSubNodes(List<SubNode> subNodes) {
            this.subNodes = subNodes;
        }
    }

    // --- Inner Class cho Ý Phụ (SubNode) ---
    public static class SubNode implements Serializable {
        private String title;
        private String details;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}