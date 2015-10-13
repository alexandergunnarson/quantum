(ns quantum.ai.core
  (:require-quantum [:lib]))

; Last year Wired quoted an ex-Google employee as saying that
; “Everything in the company is really driven by machine learning.”

; FROM QUORA:

; python - NLTK, numpy, scypi mature support but it is slow,
; can be 50 times slower than java.
; Scala recently became good repository with libraries ScalaNLP, Factore, Stanford Topic models. It's very expressive and fast.

; For quick prototyping and experimentation,
; R's CRAN system is hard to beat -- you can find, install,
; and use a new library in less than a minute (seriously!).
; Matlab is good as well.

; But these aren't good for large-scale or maintainable solutions.
; Java seems to have some open-source library momentum right now,
; though I go back to C++ myself.  Python is also worth considering.

; Don't forget Fortran!  ARPACK is still one of the best parallel
; SVD solvers around.  Python has a wrapper to this.
; And a lot of Matlab code is Fortran under the hood.

; If you are planning for large scale deployment of your learning
; algorithm, you can sort of get your learning algorithms work
; quickly using octave. Then you can spend your time re-implementing
; the algorithm to C++, Java or some of the language like that.

; For large-scale (e.g. not academic or prototypes) machine learning,
; Java probably has the lead, despite its memory inefficiencies. 
; See Mahout, which runs on Hadoop.

; JAVA SOLUTIONS
; Apache Mahout: http://mahout.apache.org
; Weka:          http://www.cs.waikato.ac.nz/ml/weka/
; Mallet:        http://mallet.cs.umass.edu

; FROM HARVARD BUSINESS REVIEW

; The most common application of machine learning tools is to make predictions. Here are a few examples of prediction problems in a business:

; Making personalized recommendations for customers
; Forecasting long-term customer loyalty
; Anticipating the future performance of employees
; Rating the credit risk of loan applicants

; The right decision might depend on a lot of variables (which means they require “wide” data).

; In ML you’re not focused on causality.
; Instead you are focusing on prediction, which means you might only
; need a model of the environment to make the right decision.
; This is just like deciding whether to leave the house with an
; umbrella: we have to predict the weather before we decide whether
; to bring one. The weather forecast is very helpful but it is limited;
; the forecast might not tell you how clouds work, or how the umbrella
; works, and it won’t tell you how to change the weather.
; The same goes for machine learning: personalized recommendations
; are forecasts of people’s preferences, and they are helpful,
; even if they won’t tell you why people like the things they do,
; or how to change what they like. If you keep these limitations
; in mind, the value of machine learning will be a lot more obvious.

; AWS

; data analysis
;   computes and visualizes your data’s distribution
;   and suggests transformations that optimize the model training process
; model training
;   finds and stores the predictive patterns within the transformed data
; evaluation
;   the model is evaluated for accuracy

; Machine learning (ML) can help you use historical data to make better
; business decisions. ML algorithms discover patterns in data and
; construct predictive models using these patterns. Then, you can
; use the models to make predictions on future data. For example,
; one possible application of ML would be to predict whether or
; not a customer will purchase a particular product based on past
; behavior, and use this prediction to send a personalized promotional 
; email to that customer.

; Separating the signal from the noise
; 1) feature extraction, which determines what data to use in the model;;
; 2) regularization, which determines how the data are weighted within the model;
; 3) cross-validation, which tests the accuracy of the model.
; Each of these factors helps us identify and separate “signal” from “noise”.

; Feature extraction

; The process of figuring out what variables the model will use. Sometimes this can simply mean dumping all the raw data straight in, but many machine learning techniques can build new variables — called “features” — which can aggregate important signals that are spread out over many variables in the raw data. In this case the signal would be too diluted to have an effect without feature extraction. One example of feature extraction is in face recognition, where the “features” are actual facial features — nose length, eye color, skin tone, etc. — that are calculated with information from many different pixels in an image. In a music store, you could have features for different genres. For instance, you could combine all the rock sales into a single feature, all the classical sales into another feature, and so on.

; There are many different ways to extract features, and the most useful ones are often automated. That means that rather than hand-picking the genre for each album, you can find “clusters” of albums that tend to be bought by all the same people, and learn the “genres” from the data (and you might even discover new genres you didn’t know existed). This is also very common with text data, where you can extract underlying “topics” of discussion based on which words and phrases tend to appear together in the same documents. However, domain experts can still be helpful in suggesting features, and in making sense of the clusters that the machine finds.

; (Clustering is a complex problem, and sometimes these tools are used just to organize data, rather than make a prediction. This type of machine learning is called “unsupervised learning”, because there is no measured outcome that is being used as a target for prediction.)

; Regularization

; How do you know if the features you’ve extracted actually reflect signal rather than noise? Intuitively, you want to tell your model to play it safe, not to jump to any conclusions. This idea is called “regularization.” (The same idea is reflected in terms like “pruning”, or “shrinkage”, or “variable selection.”) To illustrate, imagine the most conservative model possible: it would make the same prediction for everyone. In a music store, for example, this means recommending the most popular album to every person, no matter what else they liked. This approach deliberately ignores both signal and noise. At the other end of the spectrum, we could build a complex, flexible model that tries to accommodate every little quirk in a customer’s data. This model would learn from both signal and noise. The problem is, if there’s too much noise in your data, the flexible model could be even worse than the conservative baseline. This is called “over-fitting”: the model is learning patterns that won’t hold up in future cases.
; Regularization is a way to split the difference between a flexible model and a conservative model, and this is usually calculated by adding a “penalty for complexity” which forces the model to stay simple. There are two kinds of effects that this penalty can have on a model. One effect, “selection”, is when the algorithm focuses on only a few features that contain the best signal, and discards the others. Another effect, “shrinkage”, is when the algorithm reduces each feature’s influence, so that the predictions aren’t overly reliant on any one feature in case it turns out to be noisy. There are many flavors of regularization, but the most popular one, called “LASSO”, is a simple way to combine both selection and shrinkage, and it’s probably a good default for most applications.

; Cross-validation

; Once you have built a model, how can you be sure it is making good predictions? The most important test is whether the model is accurate “out of sample”, which is when the model is making predictions for data it has never seen before. This is important because eventually you will want to use the model to make new decisions, and you need to know it can do that reliably. However, it can be costly to run tests in the field, and you can be a lot more efficient by using the data you already have to simulate an “out of sample” test of prediction accuracy. This is most commonly done in machine learning with a process called “cross-validation”.
; Imagine we are building a prediction model using data on 10,000 past customers and we want to know how accurate the predictions will be for future customers. A simple way to estimate that accuracy is to randomly split the sample into two parts: a “training set” of 9,000 to build the model and a “test set” of 1,000, that is initially put aside. Once we’ve finished building a model with the training set, we can see how well the model predicts the outcomes in the test set, as a dry run. The most important thing is that model never sees the test set outcomes until after the model is built. This ensures that the test set is truly “held-out” data. If you don’t keep a clear partition between these two, you will overestimate how good your model actually is, and this can be a very costly mistake to make.

; Mistakes to avoid when using machine learning

; Don't confuse a prediction model with a causal model.
; Humans are hard-wired to think about how to change the environment to cause an effect.
; We’re just trying to optimize a decision that depends on a stable environment.
; The more stable an environment, the more useful a prediction model will be.

; It’s important to draw a distinction between “out-of-sample” and “out-of-context”. Measuring out-of-sample accuracy means that if we collect new data from the exact same environment, the model will be able to predict the outcomes well. However, there is no guarantee the model will be as useful if we move to a new environment. For example, an online store might use a database of online purchases to build a helpful model for new customers. But the exact same model may not be helpful for customers in a brick-and-mortar store – even if the product line is identical.

; The sheer size of data available will not get around the issue.
; These algorithms draw their power from being able to compare new
; cases to a large database of similar cases from the past.
; When you try to apply a model in a different context,
; the cases in the database may not be so similar any more,
; and what was a strength in the original context is now a liability.
; There’s no easy answer to this problem. An out-of-context model can still be an improvement over no model at all, as long as its limitations are taken into consideration.

; It still takes a healthy dose of human judgment to figure out where
; a model will be useful. Furthermore, there’s a lot of critical
; thinking that goes into making sure the built-in safeguards of
; regularization and cross-validation are being used the right way.

; Purely human judgment comes with its own set of biases and errors.
; With the right mix of technical skill and human judgment,
; machine learning can be a useful tool.



; http://www.skytree.net/
; Until now, it has been difficult, if not impossible, to build
; machine-learning models from large-scale diverse data sets,
; despite the proliferation of unstructured data, like text data,
; within enterprises.
; Skytree 15.2 is the first tool able to break up the schema of
; text and extract meaningful tokens for analysis, automatically,
; and in a highly secure and available environment.
; So says Alexander Gray, Ph.D., CTO and co-founder of Skytree.

; Often unstructured data requires an inordinate amount of time
; to structure and prepare data for analysis. Skytree 15.2
; simplifies the featurization of text data from sources that
; include Word, Excel, PowerPoint, PDF, raw text, tweets and
; Web pages. Machine-learning models can be built with standalone
; or fused structured and unstructured data.

; Facebook has launched a rival to Siri.
; It is a project called M - it is indeed an assistant to which
; you can ask just about any question or to solve any problem.
; Siri, Cortana, Google Now - most of the answers they provide
; are scripted. Someone has imagined the possible answers and
; figured out a tree of possibilities.