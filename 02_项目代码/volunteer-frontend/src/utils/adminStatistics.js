const emptyGraphic = (text) => ({
  graphic: [{
    type: 'text',
    left: 'center',
    top: 'middle',
    silent: true,
    style: {
      text,
      fill: '#94a3b8',
      fontSize: 14
    }
  }]
})

export function buildDistributionOption(items, axisName) {
  if (!items?.length) {
    return {
      ...emptyGraphic('暂无统计数据'),
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 18, top: 28, bottom: 42 },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.label),
      axisLabel: { color: '#64748b' }
    },
    yAxis: {
      type: 'value',
      name: axisName,
      minInterval: 1,
      axisLabel: { color: '#64748b' }
    },
    series: [{
      name: axisName,
      type: 'bar',
      barMaxWidth: 42,
      data: items.map((item) => item.count),
      itemStyle: {
        color: '#1f4e79',
        borderRadius: [6, 6, 0, 0]
      }
    }]
  }
}

export function buildYearTrendOption(trends) {
  if (!trends?.length) {
    return {
      ...emptyGraphic('暂无录取趋势数据'),
      xAxis: { type: 'category', data: [] },
      yAxis: [{ type: 'value' }],
      series: []
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      data: ['记录数量', '平均最低分', '平均最低位次']
    },
    grid: { left: 48, right: 132, top: 52, bottom: 42 },
    xAxis: {
      type: 'category',
      data: trends.map((item) => item.year),
      axisLabel: { color: '#64748b' }
    },
    yAxis: [
      {
        type: 'value',
        name: '记录数',
        minInterval: 1,
        axisLabel: { color: '#64748b' }
      },
      {
        type: 'value',
        name: '最低分',
        position: 'right',
        axisLabel: { color: '#64748b' }
      },
      {
        type: 'value',
        name: '最低位次',
        position: 'right',
        offset: 64,
        axisLabel: { color: '#64748b' }
      }
    ],
    series: [
      {
        name: '记录数量',
        type: 'bar',
        yAxisIndex: 0,
        barMaxWidth: 34,
        data: trends.map((item) => item.recordCount),
        itemStyle: { color: '#9bb7cf', borderRadius: [5, 5, 0, 0] }
      },
      {
        name: '平均最低分',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: trends.map((item) => item.averageMinScore),
        itemStyle: { color: '#1f4e79' }
      },
      {
        name: '平均最低位次',
        type: 'line',
        yAxisIndex: 2,
        smooth: true,
        data: trends.map((item) => item.averageMinRank),
        itemStyle: { color: '#cf9c47' }
      }
    ]
  }
}

export function buildFavoriteRows(items) {
  return (items || []).map((item, index) => ({
    ...item,
    rank: index + 1
  }))
}

export function buildCompletenessItems(data) {
  return [
    {
      label: '缺少专业的院校',
      count: data?.universitiesWithoutMajors || 0,
      status: data?.universitiesWithoutMajors > 0 ? 'warning' : 'success'
    },
    {
      label: '缺少录取记录的院校',
      count: data?.universitiesWithoutAdmissions || 0,
      status: data?.universitiesWithoutAdmissions > 0 ? 'warning' : 'success'
    },
    {
      label: '缺少录取记录的专业',
      count: data?.majorsWithoutAdmissions || 0,
      status: data?.majorsWithoutAdmissions > 0 ? 'warning' : 'success'
    }
  ]
}
