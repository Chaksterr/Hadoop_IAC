from pyspark import SparkContext

sc = SparkContext(appName="MatrixMultiply")
# Read files
M = sc.textFile("hdfs:///user/hadoop/multiplication/M.txt") \
    .map(lambda line: line.split(";")) \
    .map(lambda x: (int(x[1]), (int(x[0]), float(x[2])))) \
    .cache() 
N = sc.textFile("hdfs:///user/hadoop/multiplication/N.txt") \
    .map(lambda line: line.split(";")) \
    .map(lambda x: (int(x[0]), (int(x[1]), float(x[2])))) \
    .cache()

#Multiplication process 
result = M.join(N) \
    .map(lambda x: ((x[1][0][0], x[1][1][0]), x[1][0][1] * x[1][1][1])) \
    .reduceByKey(lambda a, b: a + b)

# Save the result 
output = result.sortByKey().map(lambda x: f"{x[0][0]};{x[0][1]};{x[1]}")
output.saveAsTextFile("hdfs:///user/hadoop/output/matrix_result")

sc.stop()
