// 将院校专业录取数据转换为 ECharts 双轴折线图配置。
export function buildAdmissionTrendOption(admissionPoints = [], majorId) {
  const points = admissionPoints
    .filter((point) => point.majorId === majorId)
    .sort((left, right) => left.year - right.year)

  if (!points.length) {
    return {
      graphic: [{
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '当前专业暂无可展示的录取数据',
          fill: '#8a95a6',
          fontSize: 14
        }
      }],
      xAxis: { show: false },
      yAxis: { show: false },
      series: []
    }
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      data: ['最低分', '最低位次']
    },
    grid: {
      left: 54,
      right: 62,
      top: 52,
      bottom: 36
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((point) => `${point.year}年`)
    },
    yAxis: [
      {
        type: 'value',
        name: '最低分',
        scale: true
      },
      {
        type: 'value',
        name: '最低位次',
        inverse: true,
        scale: true
      }
    ],
    series: [
      {
        name: '最低分',
        type: 'line',
        smooth: true,
        data: points.map((point) => point.minScore),
        symbolSize: 8,
        lineStyle: { width: 3 },
        itemStyle: { color: '#1f4e79' }
      },
      {
        name: '最低位次',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: points.map((point) => point.minRank),
        symbolSize: 8,
        lineStyle: { width: 3 },
        itemStyle: { color: '#c58b2a' }
      }
    ]
  }
}
