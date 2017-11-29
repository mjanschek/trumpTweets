import pandas as pd
import numpy as np
import datetime
from datetime import timedelta


workDir             = "./"             # The main working directory
comboHistoryFile    = workDir + "comboHistory.csv"     # path to comboHistory csv file
outputFile          = workDir + "timePredictions.csv"  # path to timePredictions csv file

# read comboHistory csv file
comboHistory = pd.read_csv(comboHistoryFile,
                           sep=";",
                           header = 0)

# transform timestamp strings to datetime format, add +1 for CET Winter
comboHistory['timestamp'] = pd.to_datetime(comboHistory['timestamp']) + timedelta(hours=1)

# make predictions by calculatin mean metrics for every second of a day
newComboHistory         = comboHistory.copy()
newComboHistory['time'] = comboHistory['timestamp'].apply(lambda x: x.time())
timePredictions         = newComboHistory.groupby(['time',
                                                   'isTrumpTweet',
                                                   'isNewsTweet',
                                                   'isFakeNewsTweet',
                                                   'isDemocratsTweet',
                                                   'isPoliticsTweet'])['count',
                                                                       'meanTextLength',
                                                                       'totalHashtagCount',
                                                                       'totalTrumpCount',
                                                                       'totalSensitiveCount'].agg(np.mean).reset_index()

# write timePredictions csv file
timePredictions.to_csv(outputFile,
                       sep=';',
                       quotechar='"',
                       quoting=2,
                       index=False)