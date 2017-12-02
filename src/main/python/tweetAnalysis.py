import pandas as pd
import numpy as np
import datetime
from datetime import timedelta
import time

start = time.time()

workDir             = "./"             # The main working directory
comboHistoryFile    = workDir + "comboHistory.csv"     # path to comboHistory csv file
outputFile          = workDir + "timePredictions.csv"  # path to timePredictions csv file

# read comboHistory csv file
comboHistory = pd.read_csv(comboHistoryFile,
                           sep=";",
                           header = 0)

# transform timestamp strings to datetime format, add +1 for CET Winter
comboHistory['time'] = pd.to_datetime(comboHistory['time']).apply(lambda x: x.time())

# make predictions by calculatin mean metrics for every second of a day
newComboHistory         = comboHistory.copy()
timePredictions         = newComboHistory.groupby(['time',
                                                   'isTrumpTweet',
                                                   'isNewsTweet',
                                                   'isFakeNewsTweet',
                                                   'isDemocratsTweet',
                                                   'isPoliticsTweet'])['count',
                                                                       'meanTextLength',
                                                                       'meanHashtagCount',
                                                                       'meanTrumpCount',
                                                                       'meanSensitiveCount'].agg(np.mean).reset_index()

print("Number of predictions: " + str(len(timePredictions)))

# write timePredictions csv file
timePredictions.to_csv(outputFile,
                       sep=';',
                       quotechar='"',
                       quoting=2,
                       index=False)


end = time.time()
timediffMin= (end - start)*1000*60
print("Whole process took " + str(timediffMin) + " minutes.")