import pandas as pd
import tweepy
import datetime as dt
import sys
import getopt
import time
import updateAnalysis


def updateTweets(inFile='/home/garg/tweets/filteredTweets.csv',
                 outFile='/home/garg/tweets/filteredTweetsUpdated.csv',
                 n=0,
                 noTimeout=False):

    # read tweets into pandas
    tweets = pd.read_csv(inFile,
                         sep=';',
                         header=0,
                         quoting=2)

    ### Fieldnames are:
    # 'timestamp'
    # 'userId'
    # 'userName'
    # 'followers'
    # 'tweetId'
    # 'hashTagString'
    # 'userIdsMentioned'
    # 'favorites'
    # 'retweets'
    # 'place'
    # 'text'
    # 'textLength'
    # 'isTrumpTweet'
    # 'isNewsTweet'
    # 'isFakeNewsTweet'
    # 'isDemocratsTweet'
    # 'isWashingtonDCTweet'

    # get only unique tweets, use id
    oldLen = len(tweets)
    tweets = tweets.drop_duplicates(subset='tweetId')
    newLen = len(tweets)

    print("Removed", oldLen-newLen, "duplicates.")

    consumer_key = 'lwIwe4IWCERKF116lHshZEB5Q'
    consumer_secret = 'nYn9B3crAEUV8u0UpvTkglDjYHtzA1pRZBFxSs6xUICUw4WmMt'
    access_key = '930801119051202561-p13SaAeA5VR7dqb9qZxY1fBscDwc4JH'
    access_secret = '58pQtRMDiERdXNxLKn6jHzVrjDAzVyha7KyBkD57OSvGf'
    auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
    auth.set_access_token(access_key, access_secret)
    api = tweepy.API(auth)

    # get current retweets and favorites of tweets
    # twitter search API only allows 100 id's per request

    if n > 0:
        print("Using only first", n, "rows.")
        if n > len(tweets):
            n = len(tweets)
    else:
        n = len(tweets)

    subTweets = tweets[0:n]
    subTweets['timestamp'] = pd.to_datetime(subTweets['timestamp'])
    subTweets['updatedTimestamp'] = pd.to_datetime(subTweets['timestamp'])
    subTweets['secondsOld'] = pd.Series()

    requestTimeout = 1/(450/15/60)
    requestCount = int(n/100)+1

    print("### Request information for", n, "tweets: ###")
    print("- that's", requestCount, "requests")
    if not noTimeout:
        print("- request timeout is set to", requestTimeout, "second (maximum Twitter API Requests are 450/15min)")
        print("=> This will take at least", requestCount*requestTimeout/60, "minutes!")
    for i in range(0, n, 100):
        j = i + 100
        if n - i < 100:
            j = n
        request = subTweets.tweetId[i:j].astype(str).tolist()

        # API-request sometime has internal errors and needs to be retried
        attempts = 0
        done = False
        while not done:
            try:
                tweetSet = api.statuses_lookup(request)
                done = True
            except tweepy.error.TweepError as e:
                attempts += 1
                if attempts == 5:
                    print("Request failed 5 times, abort and save to file")
                    subTweets.to_csv(outFile,
                                     sep=';',
                                     quotechar='"',
                                     quoting=2,
                                     index=False)
                    sys.exit()
                raise e

        uts = dt.datetime.now()
        subTweets['updatedTimestamp'][i:j] = uts
        for tweet in tweetSet:
            subTweets.loc[tweets['tweetId'] == tweet.id, ('retweets',
                                                          'favorites')] = (tweet.retweet_count,
                                                                           tweet.favorite_count)
        if not noTimeout:
            time.sleep(requestTimeout)


    subTweets['secondsOld'] = subTweets.apply(lambda x: (x['updatedTimestamp'] - x['timestamp']).seconds, axis=1)
    subTweets['retweetsPerSecond'] = subTweets.apply(lambda x: x['retweets'] / x['secondsOld'], axis=1)
    subTweets['favoritesPerSecond'] = subTweets.apply(lambda x: x['favorites'] / x['secondsOld'], axis=1)

    subTweets.to_csv(outFile,
                     sep=';',
                     quotechar='"',
                     quoting=2,
                     index=False)
    print('Updated', outFile)


def updateTweetsLoop(dir):
    allinputfile = dir + '/allTweets.csv'
    alloutputfile = dir + '/allTweetsUpdated.csv'
    filterinputfile = dir + '/filteredTweets.csv'
    filteroutputfile = dir + '/filteredTweetsUpdated.csv'

    while True:
        print("Updating", filteroutputfile, "...")
        updateTweets(inFile=filterinputfile,
                     outFile=filteroutputfile)
        print("Updating analysis...")
        updateAnalysis.updateAnalysis(filteredTweetsRepo=filteroutputfile,
                                      allTweetsRepo=alloutputfile,
                                      mainDir=dir)
        print("Updating", alloutputfile, "...")
        updateTweets(inFile=allinputfile,
                     outFile=alloutputfile)
        print("Updating analysis...")
        updateAnalysis.updateAnalysis(filteredTweetsRepo=filteroutputfile,
                                      allTweetsRepo=alloutputfile,
                                      mainDir=dir)


def main(argv):
    dir = '/home/garg/tweets'
    loop = False
    analysis = False
    inputfile = '/home/garg/tweets/allTweets.csv'
    outputfile = '/home/garg/tweets/allTweetsUpdated.csv'
    ntweets = 0

    infoString = 'updateTweets.py -i <inputfile> -o <outputfile> -n <ntweets> -d <dir> -l <loop> -a <analysis>'
    try:
        opts, args = getopt.getopt(argv,
                                   "hi:o:n:d:la",
                                   ["ifile=",
                                    "ofile=",
                                    "ntweets=",
                                    "dir=",
                                    "loop=",
                                    "analysis="])
    except getopt.GetoptError:
        print(infoString)
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print(infoString)
            sys.exit()
        elif opt in ("-i", "--ifile"):\
            inputfile = arg
        elif opt in ("-o", "--ofile"):\
            outputfile = arg
        elif opt in ("-n", "--ntweets"): \
            ntweets = int(arg)
        elif opt in ("-d", "--dir"): \
            dir = arg
        elif opt in ("-l", "--loop"): \
            loop = True
        elif opt in ("-a", "--analysis"): \
            analysis = True

    if loop:
        updateTweetsLoop(dir=dir)
    elif analysis:
        updateAnalysis.updateAnalysis(mainDir=dir)
    else:
        updateTweets(inFile=inputfile,
                     outFile=outputfile,
                     n=ntweets,
                     noTimeout=True)


if __name__ == "__main__":
    main(sys.argv[1:])
