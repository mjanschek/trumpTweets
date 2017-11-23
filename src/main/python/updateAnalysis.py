import pandas as pd
import numpy as np
import collections
import datetime

import plotly.offline as py
import plotly.graph_objs as go


def updateAnalysis(filteredTweetsRepo="/home/garg/tweets/filteredTweetsUpdated.csv",
                   allTweetsRepo="/home/garg/tweets/allTweetsUpdated.csv",
                   mainDir="/home/garg/tweets"):

    plotDir = mainDir + "/plots/"

    filteredTweets = pd.read_csv(filteredTweetsRepo,
                                 sep=";",
                                 header=0)
    allTweets = pd.read_csv(allTweetsRepo,
                            sep=";",
                            header=0)


    print("Rendering plots...")
    plotCompRetweets(filteredTweets,
                     allTweets,
                     plotDir)
    plotCompFavorites(filteredTweets,
                      allTweets,
                      plotDir)

    plotTextLengthComparison(filteredTweets.loc[filteredTweets['isTrumpTweet']],
                             allTweets,
                             'trump tweets',
                             'all tweets',
                             plotDir)
    plotTextLengthComparison(filteredTweets.loc[filteredTweets['isNewsTweet']],
                             allTweets,
                             'news tweets',
                             'all tweets',
                             plotDir)
    plotTextLengthComparison(filteredTweets.loc[filteredTweets['isFakeNewsTweet']],
                             allTweets,
                             'fakenews tweets',
                             'all tweets',
                             plotDir)
    plotTextLengthComparison(filteredTweets.loc[filteredTweets['isDemocratsTweet']],
                             allTweets,
                             'democrats tweets',
                             'all tweets',
                             plotDir)
    plotTextLengthComparison(filteredTweets.loc[filteredTweets['isWashingtonDCTweet']],
                             allTweets,
                             'washington tweets',
                             'all tweets',
                             plotDir)

    plotMetricPerSecond(filteredTweets,
                        allTweets,
                        fieldName='retweetsPerSecond',
                        title='Retweets per second',
                        xtitle='retweets/second',
                        plotDir=plotDir)

    plotMetricPerSecond(filteredTweets,
                        allTweets,
                        fieldName='favoritesPerSecond',
                        title='Favorites per second',
                        xtitle='favorites/second',
                        plotDir=plotDir)

    (x, y) = countTags(allTweets)
    probAllHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probAllHashTags, 'all hashtags', plotDir)

    (x, y) = countOtherTags(allTweets, 'isTrumpTweet')
    probTrumpHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probTrumpHashTags, 'hashtags co-occuring with #trump', plotDir)

    (x, y) = countOtherTags(allTweets, 'isNewsTweet')
    probNewsHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probNewsHashTags, 'hashtags co-occuring with #news', plotDir)

    (x, y) = countOtherTags(allTweets, 'isFakeNewsTweet')
    probFakeNewsHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probFakeNewsHashTags, 'hashtags co-occuring with #fakenews', plotDir)

    (x, y) = countOtherTags(allTweets, 'isDemocratsTweet')
    probDemocratsHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probDemocratsHashTags, 'hashtags co-occuring with #democrats', plotDir)

    (x, y) = countOtherTags(allTweets, 'isWashingtonDCTweet')
    probWdcHashTags = pd.DataFrame({'hashtag': x, 'probability': y})
    plotTopHashTags(probWdcHashTags, 'hashtags co-occuring with #washingtondc', plotDir)

    print("DONE!")

    tweetClasses = allTweets.loc[:,
                   ['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet']]
    tweetCombinations = tweetClasses.groupby(
        ['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet']).size().to_frame(
        'size').reset_index()
    tweetCombinations['sizeNormalized'] = tweetCombinations['size'] / len(allTweets)

    meanDF = allTweets.groupby(['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet'])[
        'textLength'].mean().to_frame('meanTextLength').reset_index()
    tweetCombinations = tweetCombinations.join(meanDF['meanTextLength'])
    meanDF = allTweets.groupby(['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet'])[
        'favoritesPerSecond'].mean().to_frame('meanFavPerSec').reset_index()
    tweetCombinations = tweetCombinations.join(meanDF['meanFavPerSec'])
    meanDF = allTweets.groupby(['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet'])[
        'retweetsPerSecond'].mean().to_frame('meanRTPerSec').reset_index()
    tweetCombinations = tweetCombinations.join(meanDF['meanRTPerSec'])

    allTweets['timestamp'] = pd.to_datetime(allTweets['timestamp'])
    allTweets['tsMinute'] = allTweets['timestamp'].apply(lambda x: x.replace(second=0, microsecond=0))
    meanAllTweetsPerMinute = allTweets.groupby('tsMinute').size().mean()
    tpmDF = allTweets.groupby(
        ['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet',
         'tsMinute']).size().to_frame('tweetsPerMinute').reset_index()
    tpmDF = tpmDF.groupby(['isTrumpTweet', 'isNewsTweet', 'isFakeNewsTweet', 'isDemocratsTweet', 'isWashingtonDCTweet'])[
        'tweetsPerMinute'].mean().to_frame('meanTweetsPerMinute').reset_index()
    tweetCombinations = tweetCombinations.join(tpmDF['meanTweetsPerMinute'])

    probAllHashTagsOccurences = probAllHashTags
    probAllHashTagsOccurences = probAllHashTagsOccurences.set_index('hashtag')
    probTrumpHashTags = probTrumpHashTags.set_index('hashtag')
    probNewsHashTags = probNewsHashTags.set_index('hashtag')
    probFakeNewsHashTags = probFakeNewsHashTags.set_index('hashtag')
    probDemocratsHashTags = probDemocratsHashTags.set_index('hashtag')
    probWdcHashTags = probWdcHashTags.set_index('hashtag')

    probAllHashTagsOccurences = probAllHashTagsOccurences.join(probTrumpHashTags['probability'], rsuffix='_trump')
    probAllHashTagsOccurences = probAllHashTagsOccurences.join(probNewsHashTags['probability'], rsuffix='_news')
    probAllHashTagsOccurences = probAllHashTagsOccurences.join(probFakeNewsHashTags['probability'], rsuffix='_fakenews')
    probAllHashTagsOccurences = probAllHashTagsOccurences.join(probDemocratsHashTags['probability'],
                                                               rsuffix='_democrats')
    probAllHashTagsOccurences = probAllHashTagsOccurences.join(probWdcHashTags['probability'], rsuffix='_washingtondc')

    savePredictionFiles(probAllHashTagsOccurences,
                        tweetCombinations,
                        mainDir)


def plotCompRetweets(filteredTweets,
                     allTweets,
                     plotDir):

    trace1 = go.Histogram(
        x=filteredTweets[filteredTweets['retweets'] > 0]['retweets'],
        opacity=0.75,
        histnorm='probability',
        name='filtered tweets'
    )
    trace2 = go.Histogram(
        x=allTweets[allTweets['retweets'] > 0]['retweets'],
        opacity=0.75,
        histnorm='probability',
        name='all tweets'
    )

    data = [trace1, trace2]
    layout = go.Layout(barmode='overlay',
                       title='All vs. filtered tweets: retweets',
                       xaxis=dict(
                           title='number of retweets',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           ),
                           range=[0, 10000]
                       ),
                       yaxis=dict(
                           title='frequency (cap at 0.05)',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           ),
                           range=[0, 0.05]
                       ))

    fig = go.Figure(data=data, layout=layout)

    py.plot(fig,
            auto_open=False,
            image='png',
            image_filename='comp_retweets',
            output_type='file',
            filename=plotDir + 'comp_retweets.html',
            validate=False)

    print("- updated", plotDir + 'comp_retweets.html')


def plotCompFavorites(filteredTweets,
                     allTweets,
                     plotDir):

    trace1 = go.Histogram(
        x=filteredTweets[filteredTweets['favorites']>0]['favorites'],
        opacity=0.75,
        histnorm='probability',
        name='filtered tweets'
    )
    trace2 = go.Histogram(
        x=allTweets[allTweets['favorites']>0]['favorites'],
        opacity=0.75,
        histnorm='probability',
        name='all tweets'
    )

    data = [trace1, trace2]
    layout = go.Layout(barmode='overlay',
                       title='All vs. filtered tweets: favorites',
                       xaxis=dict(
                           title='number of favorites',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           ),
                           range=[1,25000]
                       ),
                       yaxis=dict(
                           title='frequency (cap at 0.05)',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           ),
                           range=[0,0.05]
                       ))
    fig = go.Figure(data=data, layout=layout)

    py.plot(fig,
            auto_open=False,
            image = 'png',
            image_filename='comp_favorites' ,
            output_type='file',
            filename=plotDir + 'comp_favorites.html',
            validate=False)

    print("- updated", plotDir + 'comp_favorites.html')


def plotTextLengthComparison(df1,
                             df2,
                             name1,
                             name2,
                             plotDir):
    trace0 = go.Histogram(
        x=df1['textLength'],
        nbinsx=140,
        opacity=0.75,
        histnorm='probability',
        name=name1,
        xbins=dict(
            start=1,
            end=140,
            size=1
        )
    )

    allTrace = go.Histogram(
        x=df2['textLength'],
        nbinsx=140,
        opacity=0.75,
        histnorm='probability',
        name=name2,
        xbins=dict(
            start=1,
            end=140,
            size=1
        )
    )

    data = [trace0, allTrace]
    layout = go.Layout(barmode='overlay',
                       title='Tweet length',
                       xaxis=dict(
                           title='length of tweet-text',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ),
                       yaxis=dict(
                           title='relative frequency',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ))
    fig = go.Figure(data=data, layout=layout)

    py.plot(fig,
            auto_open=False,
            image='png',
            image_filename='tl_' + name1.replace(' ', '_'),
            output_type='file',
            filename=plotDir + 'tl_' + name1.replace(" ", "_") + '.html',
            validate=False)

    print("- updated", plotDir + 'tl_' + name1.replace(" ", "_") + '.html')


def plotMetricPerSecond(df1,
                        df2,
                        fieldName,
                        title,
                        xtitle,
                        plotDir):
    trace1 = go.Histogram(
        x=df1[df1[fieldName] > 0.1][fieldName],
        opacity=0.75,
        histnorm='probability',
        name='filtered tweets'
    )

    trace2 = go.Histogram(
        x=df2[df2[fieldName] > 0.1][fieldName],
        opacity=0.75,
        histnorm='probability',
        name='all tweets'
    )

    data = [trace1, trace2]
    layout = go.Layout(barmode='overlay',
                       title=title,
                       xaxis=dict(
                           title=xtitle,
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ),
                       yaxis=dict(
                           title='relative frequency',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ))
    fig = go.Figure(data=data, layout=layout)

    py.plot(fig,
            auto_open=False,
            image='png',
            image_filename=fieldName,
            output_type='file',
            filename=plotDir + fieldName + '.html',
            validate=False)

    print("- updated", plotDir + fieldName + '.html')


def plotMeanBars(filteredTweets,
                 allTweets,
                 plotDir):

    trace1 = go.Bar(
        x=['mean retweets', 'mean favorites'],
        y=[np.mean(allTweets['retweets']), np.mean(allTweets['favorites'])],
        name='all tweets'
    )
    trace2 = go.Bar(
        x=['mean retweets', 'mean favorites'],
        y=[np.mean(filteredTweets['retweets']), np.mean(filteredTweets['favorites'])],
        name='filtered tweets'
    )

    data = [trace1, trace2]

    layout = go.Layout(barmode='group',
                       title='Comparison: Mean of favorites and retweets',
                       yaxis=dict(
                           title='mean value',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ))

    fig = go.Figure(data=data, layout=layout)

    py.plot(fig,
            auto_open=False,
            image='png',
            image_filename='meanMetrics',
            output_type='file',
            filename=plotDir + 'meanMetrics.html',
            validate=False)

    print("- updated", plotDir + 'meanMetrics.html')


def countTags(df):
    n = len(df)
    hashTagList = list('')
    for hashTagString in df['hashTagString']:
        if isinstance(hashTagString, float):
            continue
        tmpList = hashTagString.split(",")
        hashTagList.extend(tmpList)

    counter = collections.Counter(hashTagList)

    x = []
    y = []

    for element in counter.most_common():
        x.append(element[0])
        y.append(element[1] / n)

    return (x, y)


def countOtherTags(df,
                   flagField):
    tagged = df.loc[df[flagField]]
    n = len(tagged)
    hashTagList = list('')
    for hashTagString in tagged['hashTagString']:
        tmpList = hashTagString.split(",")
        hashTagList.extend(tmpList)

    counter = collections.Counter(hashTagList)

    x = []
    y = []

    for element in counter.most_common()[1:]:
        x.append(element[0])
        y.append(element[1] / n)

    return (x, y)


def plotTopHashTags(df,
                    plotname,
                    plotDir,
                    n=50):
    trace1 = go.Bar(
        x=df.loc[:n,'hashtag'],
        y=df.loc[:n,'probability'],
        name=plotname
    )
    data = [trace1]

    layout = go.Layout(title=plotname,
                       xaxis=dict(
                           title='hashtags',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ),
                       yaxis=dict(
                           title='probability of occurence',
                           titlefont=dict(
                               family='Courier New, monospace',
                               size=18,
                               color='#7f7f7f'
                           )
                       ))
    fig = go.Figure(data=data)

    py.plot(fig,
            auto_open=False,
            image = 'png',
            image_filename='occurence_' + plotname.replace(' ','_'),
            output_type='file',
            filename=plotDir + 'occurence_' + plotname.replace(' ','_') + '.html',
            validate=False)

    print("- updated", plotDir + 'occurence_' + plotname.replace(' ','_') + '.html')


def savePredictionFiles(probAllHashTagsOccurences,
                        tweetCombinations,
                        mainDir):
    probAllHashTagsOccurences.to_csv(mainDir + "/pred_hashTags.csv",
                                     sep=';',
                                     quotechar='"',
                                     quoting=2)

    print("Updated", mainDir + "/pred_hashTags.csv")

    tweetCombinations.to_csv(mainDir + "/pred_hashTagCombMetrics.csv",
                             sep=';',
                             quotechar='"',
                             quoting=2,
                             index=False)
    print("Updated", mainDir + "/pred_hashTagCombMetrics.csv")
