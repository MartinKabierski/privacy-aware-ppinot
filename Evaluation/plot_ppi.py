import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

def main():
    data = pd.read_csv("evaluation_PAPPI_PRIPEL_optimized.csv", sep=";")
    data['From'] = data['From'].map(lambda x: str(x)[2:7])
    print(data['From'])
    sns.set_style("ticks")
    sns.set(font_scale=2)
    compare = False

    print("PPI 1")
    plot_ppi(data, 'AvgWaitingTimeUntilAdmission', 'PPI 1: Avg waiting time until admission', 'Month', 'Time (min)', "plots/ppi1_optimized_pappi.pdf", compare)

    print("PPI 2")
    plot_ppi(data, 'AvgLengthOfStay', 'PPI 2: Avg length of stay', 'Month', 'Time (days)', "plots/ppi2_optimized_pappi.pdf", compare)

    print("PPI 3")
    plot_ppi(data, 'MaxLengthOfStay', 'PPI 3: Maximum length of stay', 'Month', 'Time (days)', "plots/ppi3_optimized_pappi.pdf", compare)

    print("PPI 4")
    plot_ppi(data, '%ReturningPatients<28Days', 'PPI 4: % returning patients', 'Month', 'Percentage',"plots/ppi4_optimized_pappi.pdf", compare)

    print("PPI 5")
    plot_ppi(data, '%AntibioticsWithinOneHour', 'PPI 5: % patients with antibiotics < 60 min', 'Month', 'Percentage', "plots/ppi5_optimized_pappi.pdf", compare)

    print("PPI 6")
    plot_ppi(data, '%LacticAcidWithinThreeHours', 'PPI 6: % patients with lactic acid < 180 min', 'Month', 'Percentage', "plots/ppi6_optimized_pappi.pdf", compare)


def plot_ppi(data, ppi_name, plot_title, x_label, y_label, save_to, compare):
    ppi_df = data.loc[(data['Kpi'] == ppi_name)]
    if not compare:
        ppi_df = ppi_df.loc[(data['Algorithm'] == 'PaPPI')]

    if compare:
        ax = sns.boxplot(x="From",
                         y="Result",
                         hue='Algorithm',
                         data=ppi_df,
                         color="lightblue"
                         )
    else:
        ax = sns.boxplot(x="From",
                         y="Result",
                         data=ppi_df,
                         color="lightblue"
                         )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title(plot_title)
    ax.tick_params(axis='both', which='major')
    ax.set_xlabel(x_label, labelpad=20)
    ax.set_ylabel(y_label)
    plt.tight_layout()
    # ax.set_ylim(-5,105)
#    if compare:
#        plt.savefig(str(save_to)+"_comparison", format="pdf")
#    else:
    plt.savefig(str(save_to), format="pdf")
    plt.show()

    return


def plot_ppi2(data):
    ppi_df = data.loc[(data['Kpi'] == 'AvgLengthOfStay')]

    ax = sns.boxplot(x="From",
                     y="Result",
                     hue='Algorithm',
                     data=ppi_df,
                     color="lightblue"
                     )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title('PPI 2: Avg length of stay')
    ax.set_xlabel('Month', labelpad=20)
    ax.set_ylabel('Time (days)')
    plt.tight_layout()
    # ax.set_ylim(-5,105)
    plt.savefig("plots/ppi2.pdf", format="pdf")
    plt.show()
    return


def plot_ppi3(data):
    ppi_df = data.loc[(data['Kpi'] == 'MaxLengthOfStay')]

    ax = sns.boxplot(x="From",
                     y="Result",
                     hue='Algorithm',
                     data=ppi_df,
                     color="lightblue"
                     )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title('PPI 3: Maximum length of stay')
    ax.set_xlabel('Month', labelpad=20)
    ax.set_ylabel('Time (days)')
    plt.tight_layout()
    # ax.set_ylim(-5,105)
    plt.savefig("plots/ppi3.pdf", format="pdf")
    plt.show()
    return


def plot_ppi4(data):
    ppi_df = data.loc[(data['Kpi'] == 'PercentageOfReturningPatients')]

    ax = sns.boxplot(x="From",
                     y="Result",
                     hue='Algorithm',
                     data=ppi_df,
                     color="lightblue"
                     )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title('PPI 4: Fraction of returning patient')
    ax.set_xlabel('Month', labelpad=20)
    ax.set_ylabel('Percentage')
    plt.tight_layout()
    ax.set_ylim(-5, 105)
    plt.savefig("plots/ppi4.pdf", format="pdf")
    plt.show()
    return


def plot_ppi5(data):
    ppi_df = data.loc[(data['Kpi'] == '%AntibioticsWithinOneHour')]

    ax = sns.boxplot(x="From",
                     y="Result",
                     hue='Algorithm',
                     data=ppi_df,
                     color="lightblue"
                     )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title('PPI 5: Fraction of patients with antibitiotcs < 60 min')
    ax.set_xlabel('Month', labelpad=20)
    ax.set_ylabel('Percentage')
    plt.tight_layout()
    ax.set_ylim(-5, 105)
    plt.savefig("plots/ppi5.pdf", format="pdf")
    plt.show()
    return


def plot_ppi6(data):
    ppi_df = data.loc[(data['Kpi'] == '%LacticAcidWithinThreeHours')]

    ax = sns.boxplot(x="From",
                     y="Result",
                     hue='Algorithm',
                     data=ppi_df,
                     color="lightblue"
                     )
    fig = plt.gcf()
    fig.set_size_inches(8, 4.8)
    plt.xticks(rotation='vertical')
    sns.lineplot(x="From",
                 y="Orig",
                 data=ppi_df)

    ax.set_title('PPI 6: Fraction of patients with lactic acid < 180 min')
    ax.set_xlabel('Month', labelpad=20)
    ax.set_ylabel('Percentage')
    plt.tight_layout()
    ax.set_ylim(0 - 5, 105)
    plt.savefig("plots/ppi6.pdf", format="pdf")
    plt.show()
    return


if __name__ == "__main__":
    main()
