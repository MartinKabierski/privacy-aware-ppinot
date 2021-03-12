import numpy as np
import random
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

def main():
    font = {'family' : 'normal',
        #'weight' : 'bold',
        'size'   : 22}

    plt.rc('font', **font)
    
    data = pd.read_csv("evaluation_params.csv",sep=";")
    sns.set(font_scale=1.5)
    sns.set_style("ticks")

    #parameter analysis
    print("Target Value")
    plot_target_value_impact(data)
    print("Falloff Factor")
    plot_falloff_factor_impact(data)
    print("Extension Factor")
    plot_extension_impact(data)
    print("No of Values")
    plot_no_of_values_impact(data)
    print("No of Values")
    plot_distribution_impact(data)

    #compare different mechanisms using ideal parameter settings for different epsilon
    print("Sum Mechanisms")
    plot_sum_comparison(data)
    print("Mean Mechanisms")
    plot_avg_comparison(data)
    print("Min & Max Mechanisms")
    plot_minmax_comparison(data)
    print("Derived Measure")
    plot_derived_comparison(data)




def plot_target_value_impact(data):
    avg_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Avg_exp_falloff') &(data['Falloff Factor']==20) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    sum_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Sum_exp_falloff') &(data['Falloff Factor']==20) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    min_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Min_exp_falloff') &(data['Falloff Factor']==20) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    max_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Max_exp_falloff') &(data['Falloff Factor']==20) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=4,constrained_layout=True)
    sns.boxplot(x="Target",
                y="Result",
                data=avg_df,
                color="lightblue",ax=axes[0])
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['w/o','-10','+10']
    axes[0].set_xticklabels(labels)
    axes[0].set_title('Mean')
    axes[0].set_ylim(0,200)


    sns.boxplot(x="Target",
                y="Result",
                data=sum_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Sum')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['w/o','-100','+100']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(0,40000)


    sns.boxplot(x="Target",
                y="Result",
                data=min_df,
                color="lightblue",ax=axes[2])
    axes[2].set_title('Min')
    labels = [item.get_text() for item in axes[2].get_xticklabels()]
    labels = ['w/o','-10','+10']
    axes[2].set_xticklabels(labels)
    axes[2].set_ylim(0,200)


    sns.boxplot(x="Target",
                y="Result",
                data=max_df,
                color="lightblue",ax=axes[3])
    axes[3].set_title('Max')
    labels = [item.get_text() for item in axes[3].get_xticklabels()]
    labels = ['w/o','Y-10','Y+10']
    axes[3].set_xticklabels(labels)
    axes[3].set_ylim(0,200)

    
    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)#, length=6, width=2)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Target Value')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        axes[0].xaxis.labelpad=8
        axes[2].xaxis.labelpad=8
        axes[3].xaxis.labelpad=8
        axes[0].axhline(97.765,color="blue",zorder=10)
        axes[1].axhline(19553.0,color="blue",zorder=10)
        axes[2].axhline(28.0,color="blue",zorder=10)
        axes[3].axhline(170.0,color="blue",zorder=10)


        #ax1.tick_params(length=6, width=2)
    #f.subplots_adjust(wspace=1.0,top=0.55)
    f.set_size_inches( 10.4, 3)
    f.show()
    f.savefig("plots/target_value.pdf",format="pdf")

    return


def plot_falloff_factor_impact(data):
    avg_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Avg_exp_falloff') &(data['Target']==87.765)  & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)& (data['NoOfValues']==200.0)]
    sum_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Sum_exp_falloff') &(data['Target']==19453.0) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)& (data['NoOfValues']==200.0)]
    min_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Min_exp_falloff') &(data['Target']==18.0)    & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)& (data['NoOfValues']==200.0)]
    max_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Max_exp_falloff') &(data['Target']==180.0)   & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)& (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=4,constrained_layout=True)
    sns.boxplot(x="Falloff Factor",
                y="Result",
                data=avg_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Mean')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['w/o','1','5','10','20']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(0,200)


    sns.boxplot(x="Falloff Factor",
                y="Result",
                data=sum_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Sum')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['w/o','1','5','10','20']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(0,40000)

    sns.boxplot(x="Falloff Factor",
                y="Result",
                data=min_df,
                color="lightblue",ax=axes[2])
    axes[2].set_title('Min')
    labels = [item.get_text() for item in axes[2].get_xticklabels()]
    labels = ['w/o','1','5','10','20']
    axes[2].set_xticklabels(labels)
    axes[2].set_ylim(0,200)

    sns.boxplot(x="Falloff Factor",
                y="Result",
                data=max_df,
                color="lightblue",ax=axes[3])
    axes[3].set_title('Max')
    labels = [item.get_text() for item in axes[3].get_xticklabels()]
    labels = ['w/o','1','5','10','20']
    axes[3].set_xticklabels(labels)
    axes[3].set_ylim(0,200)

    
    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Falloff Factor')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        axes[0].axhline(97.765,color="blue",zorder=10)
        axes[1].axhline(19553.0,color="blue",zorder=10)
        axes[2].axhline(28.0,color="blue",zorder=10)
        axes[3].axhline(170.0,color="blue",zorder=10)

        
    f.set_size_inches( 10.4, 3)
    f.show()
    f.savefig("plots/falloff_factor.pdf",format="pdf")
    return

def plot_extension_impact(data):
    avg_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Avg_exp') & (data['Falloff Factor']==20) &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['NoOfValues']==200.0)]
    sum_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Sum_exp') & (data['Falloff Factor']==20) &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['NoOfValues']==200.0)]
    min_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Min_exp') & (data['Falloff Factor']==20) &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['NoOfValues']==200.0)]
    max_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Max_exp') & (data['Falloff Factor']==20) &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=4,constrained_layout=True)
    sns.boxplot(x="Extension Factor",
                y="Result",
                data=avg_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Mean')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['w/o','15%','30%']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(-30,230)


    sns.boxplot(x="Extension Factor",
                y="Result",
                data=sum_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Sum')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['w/o','15%','30%']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(-600,40600)


    sns.boxplot(x="Extension Factor",
                y="Result",
                data=min_df,
                color="lightblue",ax=axes[2])
    axes[2].set_title('Min')
    labels = [item.get_text() for item in axes[2].get_xticklabels()]
    labels = ['w/o','15%','30%']
    axes[2].set_xticklabels(labels)
    axes[2].set_ylim(-30,230)


    sns.boxplot(x="Extension Factor",
                y="Result",
                data=max_df,
                color="lightblue",ax=axes[3])
    axes[3].set_title('Max')
    labels = [item.get_text() for item in axes[3].get_xticklabels()]
    labels = ['w/o','15%','30%']
    axes[3].set_xticklabels(labels)
    axes[3].set_ylim(-30,230)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_ylabel('')
        ax.set_xlabel('Extension Factor')
        axes[0].set_ylabel('Result')
        axes[0].axhline(97.765,color="blue",zorder=10)
        axes[1].axhline(19553.0,color="blue",zorder=10)
        axes[2].axhline(28.0,color="blue",zorder=10)
        axes[3].axhline(170.0,color="blue",zorder=10)
    f.set_size_inches( 10.4, 3)
    f.show()
    f.savefig("plots/extension_factor.pdf",format="pdf")
    return

def plot_no_of_values_impact(data):
    avg_df  = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Avg_lap') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3)]
    bavg_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Avg_exp')& (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
                y="Result",
                data=avg_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace ')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(0,200)


    sns.boxplot(x="NoOfValues",
                y="Result",
                data=bavg_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(0,200)

    
    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('No of Values')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(89.8,xmin=0,xmax=0.25,color="blue",zorder=10)
        ax.axhline(96.14,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        ax.axhline(95.34,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        ax.axhline(97.765,xmin=0.75,xmax=1.0,color="blue",zorder=10)
        
    f.set_size_inches( 5.2, 3)
    f.savefig("plots/noOfValues_mean.pdf",format="pdf")
    f.show()

    sum_df  = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Sum_lap') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3)]
    bsum_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Sum_exp')& (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
                y="Result",
                data=sum_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(0,40000)


    sns.boxplot(x="NoOfValues",
                y="Result",
                data=bsum_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(0,40000)

    
    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('No of Values')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(898.0,xmin=0,xmax=0.25,color="blue",zorder=10)
        ax.axhline(4807.0,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        ax.axhline(9534.0,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        ax.axhline(19553.0,xmin=0.75,xmax=1.0,color="blue",zorder=10)

    f.set_size_inches( 5.2, 3)
    f.savefig("plots/noOfValues_sum.pdf",format="pdf")
    f.show()
    return

def plot_distribution_impact(data):
    min_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Min_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]
    max_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='gaussian') & (data['Method']=='Max_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
                y="Result",
                data=min_exp_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Min')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(0,200)

    sns.boxplot(x="NoOfValues",
                y="Result",
                data=max_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Max')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(0,200)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('No of Values')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        axes[0].axhline(28,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[0].axhline(28,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[0].axhline(28,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[0].axhline(28,xmin=0.75,xmax=1.0,color="blue",zorder=10)
        axes[1].axhline(147,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[1].axhline(170,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[1].axhline(170,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[1].axhline(170,xmin=0.75,xmax=1.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.tight_layout()
    f.show()
    f.savefig("plots/noOfValues_minmax_gaussian.pdf",format="pdf")


    min_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='pareto') & (data['Method']=='Min_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]
    max_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='pareto') & (data['Method']=='Max_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
                y="Result",
                data=min_exp_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Min')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(-49,250)


    sns.boxplot(x="NoOfValues",
                y="Result",
                data=max_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Max')
    labels = [item.get_text() for item in axes[1].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(-49,250)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('No of Values')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        axes[0].axhline(10,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[0].axhline(10,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[0].axhline(10,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[0].axhline(10,xmin=0.75,xmax=1.0,color="blue",zorder=10)
        axes[1].axhline(100,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[1].axhline(100,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[1].axhline(212,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[1].axhline(212,xmin=0.75,xmax=1.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.tight_layout()
    f.show()
    f.savefig("plots/noOfValues_minmax_pareto.pdf",format="pdf")


    min_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='poisson') & (data['Method']=='Min_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]
    max_exp_df = data.loc[(data['Epsilon']==0.1) & (data['Distribution']=='poisson') & (data['Method']=='Max_exp') & (data['Falloff Factor']==20)  &(data['Target']==-100) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
                y="Result",
                data=min_exp_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Min')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[0].set_xticklabels(labels)
    axes[0].set_ylim(-2,17.5)


    sns.boxplot(x="NoOfValues",
                y="Result",
                data=max_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Max')
    labels = [item.get_text() for item in axes[0].get_xticklabels()]
    labels = ['10','50','100','200']
    axes[1].set_xticklabels(labels)
    axes[1].set_ylim(-2,17.5)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('No of Values')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        axes[0].axhline(1,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[0].axhline(0,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[0].axhline(0,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[0].axhline(0,xmin=0.75,xmax=1.0,color="blue",zorder=10)
        axes[1].axhline(8,xmin=0,xmax=0.25,color="blue",zorder=10)
        axes[1].axhline(10,xmin=0.25,xmax=0.5,color="blue",zorder=10)
        axes[1].axhline(10,xmin=0.5,xmax=0.75,color="blue",zorder=10)
        axes[1].axhline(14,xmin=0.75,xmax=1.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.tight_layout()
    f.show()
    f.savefig("plots/noOfValues_minmax_poisson.pdf",format="pdf")
    return


def plot_sum_comparison(data):
    sum_lap_df = data.loc[(data['Distribution']=='gaussian')& (data['Method']=='Sum_lap') & (data['Falloff Factor']==20)  &(data['Target']==19453.0) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    sum_exp_df = data.loc[(data['Distribution']=='gaussian')& (data['Method']=='Sum_exp') & (data['Falloff Factor']==20)  &(data['Target']==19453.0) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="Epsilon",
                y="Result",
                data=sum_lap_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace')
    axes[0].set_ylim(0,40000)


    sns.boxplot(x="Epsilon",
                y="Result",
                data=sum_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    axes[1].set_ylim(0,40000)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Epsilon')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(19553.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.show()
    f.savefig("plots/comparison_sum.pdf",format="pdf")
    return


def plot_avg_comparison(data):
    sum_lap_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Avg_lap')  & (data['Falloff Factor']==20)  &(data['Target']==87.765) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    sum_exp_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Avg_exp')  & (data['Falloff Factor']==20)  &(data['Target']==87.765) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="Epsilon",
                y="Result",
                data=sum_lap_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace')
    axes[0].set_ylim(0,200)


    sns.boxplot(x="Epsilon",
                y="Result",
                data=sum_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    axes[1].set_ylim(0,200)


    for ax in axes:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Epsilon')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(97.765,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.show()
    f.savefig("plots/comparison_mean.pdf",format="pdf")

    return

def plot_minmax_comparison(data):
    min_lap_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Min_lap')  & (data['Falloff Factor']==10) & (data['Target']==18.0)  & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    min_exp_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Min_exp')  & (data['Falloff Factor']==10) & (data['Target']==18.0)  & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]

    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="Epsilon",
                y="Result",
                data=min_lap_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace')
    axes[0].set_ylim(0,200)


    sns.boxplot(x="Epsilon",
                y="Result",
                data=min_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    axes[1].set_ylim(0,200)

    
    for ax in axes.flat:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Epsilon')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(28.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.show()
    f.savefig("plots/comparison_min.pdf",format="pdf")

    max_lap_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Max_lap')  & (data['Falloff Factor']==10) & (data['Target']==180.0) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    max_exp_df = data.loc[(data['Distribution']=='gaussian') & (data['Method']=='Max_exp')  & (data['Falloff Factor']==10) & (data['Target']==180.0) & (data['BoundEstimation']=='extend') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]

    
    f, axes = plt.subplots(nrows=1, ncols=2,constrained_layout=True)
    sns.boxplot(x="Epsilon",
                y="Result",
                data=max_lap_df,
                color="lightblue",ax=axes[0])
    axes[0].set_title('Laplace')
    axes[0].set_ylim(0,200)

    sns.boxplot(x="Epsilon",
                y="Result",
                data=max_exp_df,
                color="lightblue",ax=axes[1])
    axes[1].set_title('Interval')
    axes[1].set_ylim(0,200)

    
    for ax in axes.flat:
        ax.tick_params(axis='x',labelrotation=90)
        ax.tick_params(axis='both', which='major')
        ax.set_xlabel('Epsilon')
        ax.set_ylabel('')
        axes[0].set_ylabel('Result')
        ax.axhline(170.0,color="blue",zorder=10)
    f.set_size_inches( 5.2, 3)
    f.show()
    f.savefig("plots/comparison_max.pdf",format="pdf")
    return


def plot_derived_comparison(data):
    epsilon_df      = data.loc[(data['Distribution']=='binary') & (data['Method']=='Percentage') & (data['Falloff Factor']==20.0)  &(data['Target']==-100.0) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3) & (data['NoOfValues']==200.0)]
    no_of_values_df = data.loc[(data['Distribution']=='binary') & (data['Method']=='Percentage') & (data['Falloff Factor']==20.0)  &(data['Target']==-100.0) & (data['BoundEstimation']=='minmax') & (data['Extension Factor']==1.3)& (data['Epsilon']==0.1)]

    f, axes = plt.subplots(nrows=1, ncols=1,constrained_layout=True)
    sns.boxplot(x="Epsilon",
                y="Result",
                data=epsilon_df,
                color="lightblue")
    axes.set_title('Sample-and-Aggregate')
    axes.tick_params(axis='x',labelrotation=90)
    axes.tick_params(axis='both', which='major')
    axes.set_xlabel('Epsilon')
    axes.set_ylabel('Result')
    axes.axhline(50.0,color="blue",zorder=10)
    axes.set_ylim(-1,101)


    #for ax in axes:
    axes.tick_params(labelrotation=90)
    f.set_size_inches( 3, 3)
    f.show()
    f.savefig("plots/derived_epsilon.pdf",format="pdf")

    f, axes = plt.subplots(nrows=1, ncols=1,constrained_layout=True)
    sns.boxplot(x="NoOfValues",
            y="Result",
            data=no_of_values_df,
            color="lightblue")
    axes.set_title('Sample-and-Aggregate')
    axes.tick_params(axis='x',labelrotation=90)
    axes.tick_params(axis='both', which='major')
    axes.set_xlabel('No of Values')
    axes.set_ylabel('Result')
    axes.axhline(50.0,color="blue",zorder=10)
    axes.set_ylim(-1,101)

    #for ax in axes:
    axes.tick_params(labelrotation=90)
    fig = plt.gcf()
    f.set_size_inches( 3,3)
    f.show()
    f.savefig("plots/derived_noOfValues.pdf",format="pdf")


if __name__ == "__main__":
    main()
