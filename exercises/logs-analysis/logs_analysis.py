from pyspark import SparkContext

sc = SparkContext(appName="LogAnalysisOneOutput")

logs = sc.textFile("hdfs:///user/hadoop/log-analysis/logs.txt") \
    .map(lambda line: line.strip().split()) \
    .filter(lambda x: len(x) == 3) \
    .cache()

by_ip = logs.map(lambda x: (x[0], 1)).reduceByKey(lambda a, b: a + b).sortByKey()
by_method = logs.map(lambda x: (x[1], 1)).reduceByKey(lambda a, b: a + b).sortByKey()
by_os = logs.map(lambda x: (x[2], 1)).reduceByKey(lambda a, b: a + b).sortByKey()

ip_lines = by_ip.map(lambda x: f"IP {x[0]} {x[1]}")
method_lines = by_method.map(lambda x: f"METHOD {x[0]} {x[1]}")
os_lines = by_os.map(lambda x: f"OS {x[0]} {x[1]}")

all_results = ip_lines.union(method_lines).union(os_lines)

all_results.coalesce(1).saveAsTextFile("hdfs:///user/hadoop/output1/log_analysis_all")

sc.stop()