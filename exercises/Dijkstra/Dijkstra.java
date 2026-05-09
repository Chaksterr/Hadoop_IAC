package tp;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Dijkstra {

    public static class Node {

        private int distance;
        private List<String> neighbors;
        private boolean visited;

        public Node() {
            this.distance = Integer.MAX_VALUE;
            this.neighbors = new ArrayList<>();
            this.visited = false;
        }

        public Node(int distance, List<String> neighbors, boolean visited) {
            this.distance = distance;
            this.neighbors = neighbors;
            this.visited = visited;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public List<String> getNeighbors() {
            return neighbors;
        }

        public void setNeighbors(List<String> neighbors) {
            this.neighbors = neighbors;
        }

        public boolean isVisited() {
            return visited;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        // Like the desired Input format 
        public String toString() {

            StringBuilder sb = new StringBuilder();
            if (distance == Integer.MAX_VALUE) {
                sb.append("INF");
            } else {
                sb.append(distance);
            }
            sb.append("|");
            sb.append(visited ? "1" : "0").append("|");
            for (int i = 0; i < neighbors.size(); i++) {
                sb.append(neighbors.get(i));
                if (i < neighbors.size() - 1) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        // Decompose the String for creating the Node 

        public static Node fromString(String s) {
            String[] parts = s.split("\\|");
            Node node = new Node();
            
            if (parts[0].equals("INF")) {
                node.setDistance(Integer.MAX_VALUE);
            } else {
                node.setDistance(Integer.parseInt(parts[0]));
            }
            
            node.setVisited(parts[1].equals("1"));
            
            if (parts.length > 2 && !parts[2].isEmpty()) {
                String[] edges = parts[2].split(",");
                List<String> neighbors = new ArrayList<>();
                for (String edge : edges) {
                    neighbors.add(edge);
                }
                node.setNeighbors(neighbors);
            }
            
            return node;
        }
    }

    // Mapper class

    public static class DijkstraMapper extends Mapper<Object, Text, Text, Text> {

        @Override
        public void map(Object key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString();

            String[] parts = line.split("\\s+", 2);
            
            if (parts.length < 2) return;
            
            String nodeId = parts[0];
            Node node = Node.fromString(parts[1]);
            
            // Emit the node itself to preserve graph structure
            context.write(new Text(nodeId), new Text("NODE|" + node.toString()));

            // If node is not visited and has finite distance, propagate to neighbors
            if (!node.isVisited() && node.getDistance() != Integer.MAX_VALUE) {
                for (String neighbor : node.getNeighbors()) {
                    String[] neighborParts = neighbor.split(":");
                    String neighborId = neighborParts[0];
                    int edgeWeight = Integer.parseInt(neighborParts[1]);
                    int newDistance = node.getDistance() + edgeWeight;
                    
                    // Emit distance update to neighbor
                    context.write(new Text(neighborId), new Text("DIST|" + newDistance));
                }
            }
        }
    }

    // Reducer class
    public static class DijkstraReducer extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            Node node = null;
            int minDistance = Integer.MAX_VALUE;

            // Process all values for this node
            for (Text value : values) {
                String val = value.toString();
                String[] parts = val.split("\\|", 2);
                String type = parts[0];

                if (type.equals("NODE")) {
                    // Reconstruct node structure
                    node = Node.fromString(parts[1]);
                    minDistance = Math.min(minDistance, node.getDistance());
                } else if (type.equals("DIST")) {
                    // Distance update from neighbor
                    int distance = Integer.parseInt(parts[1]);
                    minDistance = Math.min(minDistance, distance);
                }
            }

            // Update node with minimum distance found
            if (node != null) {
                if (minDistance < node.getDistance()) {
                    node.setDistance(minDistance);
                    node.setVisited(false); // Mark as unvisited to process in next iteration
                } else if (!node.isVisited()) {
                    node.setVisited(true); // Mark as visited if no update
                }

                // Emit the updated node
                context.write(key, new Text(node.toString()));
            }
        }
    }

    // Main Class 
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: Main <input path> <output path> <max iterations>");
            System.exit(-1);
        }

        String inputPath = args[0];
        String outputBase = args[1];
        int maxIterations = Integer.parseInt(args[2]);

        Configuration conf = new Configuration();
        
        for (int i = 0; i < maxIterations; i++) {
            Job job = Job.getInstance(conf, "Dijkstra Iteration " + i);
            job.setJarByClass(Dijkstra.class);
            job.setMapperClass(DijkstraMapper.class);
            job.setReducerClass(DijkstraReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            String input = (i == 0) ? inputPath : outputBase + "/" + (i - 1);
            String output = outputBase + "/" + i;

            FileInputFormat.addInputPath(job, new Path(input));
            FileOutputFormat.setOutputPath(job, new Path(output));

            boolean success = job.waitForCompletion(true);
            
            if (!success) {
                System.err.println("Job failed at iteration " + i);
                System.exit(1);
            }

            System.out.println("Completed iteration " + i);
        }

        System.out.println("Dijkstra algorithm completed after " + maxIterations + " iterations");
        System.out.println("Final output in: " + outputBase + (maxIterations - 1));
    }
}