import { writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = dirname(fileURLToPath(import.meta.url))
const target = resolve(currentDir, '../database/seed/02_seed.sql')

const adminPasswordHash = '$2a$10$7R92YxFinDw4OnnkV.1M0.KXv3a9ZYuzHeZGQx1ifWPHmp/MajJ92'
const studentPasswordHash = '$2a$10$FJQdDXQ3Ry8YKSD8IfnDGuwZUpLk8AD0RLNeAo0k8wu7WA2ThF9l2'
const source = '演示数据（非官方，仅用于系统展示）'

// [名称, 省份, 城市, 层次, 类型, 分数档位]
const universities = [
  ['太原理工大学', '山西', '太原', '双一流', '理工', 2],
  ['山西大学', '山西', '太原', '双一流', '综合', 2],
  ['中北大学', '山西', '太原', '省重点', '理工', 3],
  ['山西财经大学', '山西', '太原', '省重点', '财经', 3],
  ['山西医科大学', '山西', '太原', '省重点', '医药', 3],
  ['山西师范大学', '山西', '太原', '省重点', '师范', 3],
  ['北京工业大学', '北京', '北京', '双一流', '理工', 2],
  ['北京邮电大学', '北京', '北京', '双一流', '理工', 1],
  ['中国农业大学', '北京', '北京', '双一流', '农林', 1],
  ['北京科技大学', '北京', '北京', '双一流', '理工', 2],
  ['南开大学', '天津', '天津', '双一流', '综合', 1],
  ['天津大学', '天津', '天津', '双一流', '理工', 1],
  ['河北工业大学', '天津', '天津', '双一流', '理工', 2],
  ['大连理工大学', '辽宁', '大连', '双一流', '理工', 1],
  ['东北大学', '辽宁', '沈阳', '双一流', '理工', 1],
  ['吉林大学', '吉林', '长春', '双一流', '综合', 1],
  ['哈尔滨工业大学', '黑龙江', '哈尔滨', '双一流', '理工', 1],
  ['哈尔滨工程大学', '黑龙江', '哈尔滨', '双一流', '理工', 2],
  ['复旦大学', '上海', '上海', '双一流', '综合', 1],
  ['上海交通大学', '上海', '上海', '双一流', '综合', 1],
  ['同济大学', '上海', '上海', '双一流', '理工', 1],
  ['华东师范大学', '上海', '上海', '双一流', '师范', 1],
  ['上海财经大学', '上海', '上海', '双一流', '财经', 1],
  ['南京大学', '江苏', '南京', '双一流', '综合', 1],
  ['东南大学', '江苏', '南京', '双一流', '综合', 1],
  ['南京航空航天大学', '江苏', '南京', '双一流', '理工', 2],
  ['南京理工大学', '江苏', '南京', '双一流', '理工', 2],
  ['苏州大学', '江苏', '苏州', '双一流', '综合', 2],
  ['浙江大学', '浙江', '杭州', '双一流', '综合', 1],
  ['浙江工业大学', '浙江', '杭州', '省重点', '理工', 3],
  ['中国科学技术大学', '安徽', '合肥', '双一流', '理工', 1],
  ['合肥工业大学', '安徽', '合肥', '双一流', '理工', 2],
  ['厦门大学', '福建', '厦门', '双一流', '综合', 1],
  ['福州大学', '福建', '福州', '双一流', '理工', 2],
  ['山东大学', '山东', '济南', '双一流', '综合', 1],
  ['中国海洋大学', '山东', '青岛', '双一流', '综合', 1],
  ['中国石油大学（华东）', '山东', '青岛', '双一流', '理工', 2],
  ['郑州大学', '河南', '郑州', '双一流', '综合', 2],
  ['河南大学', '河南', '开封', '双一流', '综合', 2],
  ['武汉大学', '湖北', '武汉', '双一流', '综合', 1],
  ['华中科技大学', '湖北', '武汉', '双一流', '综合', 1],
  ['武汉理工大学', '湖北', '武汉', '双一流', '理工', 2],
  ['中南大学', '湖南', '长沙', '双一流', '综合', 1],
  ['湖南大学', '湖南', '长沙', '双一流', '综合', 1],
  ['中山大学', '广东', '广州', '双一流', '综合', 1],
  ['华南理工大学', '广东', '广州', '双一流', '理工', 1],
  ['深圳大学', '广东', '深圳', '省重点', '综合', 3],
  ['暨南大学', '广东', '广州', '双一流', '综合', 2],
  ['四川大学', '四川', '成都', '双一流', '综合', 1],
  ['电子科技大学', '四川', '成都', '双一流', '理工', 1],
  ['西南交通大学', '四川', '成都', '双一流', '理工', 2],
  ['重庆大学', '重庆', '重庆', '双一流', '综合', 1],
  ['西安交通大学', '陕西', '西安', '双一流', '综合', 1],
  ['西北工业大学', '陕西', '西安', '双一流', '理工', 1],
  ['西安电子科技大学', '陕西', '西安', '双一流', '理工', 2],
  ['兰州大学', '甘肃', '兰州', '双一流', '综合', 1],
  ['新疆大学', '新疆', '乌鲁木齐', '双一流', '综合', 2],
  ['内蒙古大学', '内蒙古', '呼和浩特', '双一流', '综合', 2],
  ['南昌大学', '江西', '南昌', '双一流', '综合', 2],
  ['广西大学', '广西', '南宁', '双一流', '综合', 2],
  ['云南大学', '云南', '昆明', '双一流', '综合', 2],
  ['贵州大学', '贵州', '贵阳', '双一流', '综合', 2],
  ['海南大学', '海南', '海口', '双一流', '综合', 2],
  ['福建师范大学', '福建', '福州', '省重点', '师范', 3],
  ['山东科技大学', '山东', '青岛', '省重点', '理工', 3],
  ['河南工业大学', '河南', '郑州', '省重点', '理工', 3]
]

const majorSets = {
  理工: ['计算机科学与技术', '软件工程', '电子信息工程', '自动化'],
  综合: ['计算机科学与技术', '软件工程', '工商管理', '法学'],
  财经: ['金融学', '会计学', '经济学', '数据科学与大数据技术'],
  师范: ['汉语言文学', '数学与应用数学', '计算机科学与技术', '教育学'],
  医药: ['临床医学', '药学', '医学影像学', '预防医学'],
  农林: ['农学', '食品科学与工程', '计算机科学与技术', '生物科学']
}

const categoryByName = {
  计算机科学与技术: '工学',
  软件工程: '工学',
  电子信息工程: '工学',
  自动化: '工学',
  数据科学与大数据技术: '工学',
  工商管理: '管理学',
  会计学: '管理学',
  法学: '法学',
  金融学: '经济学',
  经济学: '经济学',
  汉语言文学: '文学',
  数学与应用数学: '理学',
  教育学: '教育学',
  临床医学: '医学',
  药学: '医学',
  医学影像学: '医学',
  预防医学: '医学',
  农学: '农学',
  食品科学与工程: '工学',
  生物科学: '理学'
}

function sql(value) {
  if (value === null || value === undefined) return 'NULL'
  return `'${String(value).replaceAll("'", "''")}'`
}

const users = [
  `(1, 'admin', ${sql(adminPasswordHash)}, '系统管理员', 'ADMIN', NULL, NULL, NULL, NULL, NULL, '2026-09-01 08:00:00', '2026-09-01 08:00:00')`,
  `(2, 'student', ${sql(studentPasswordHash)}, '示例学生', 'STUDENT', '山西', 2026, '物理类', 560, 10000, '2026-09-02 09:00:00', '2026-09-02 09:00:00')`
]
const provinces = ['山西', '陕西', '河南', '山东', '江苏', '浙江', '河北', '四川']
for (let index = 1; index <= 26; index++) {
  const id = index + 2
  const province = provinces[(index - 1) % provinces.length]
  const subjectTrack = index % 4 === 0 ? '历史类' : '物理类'
  const score = subjectTrack === '历史类' ? 485 + (index * 7) % 95 : 470 + (index * 11) % 155
  const rank = 2500 + index * 2800
  const day = String(index + 2).padStart(2, '0')
  users.push(
    `(${id}, 'student${String(index).padStart(2, '0')}', ${sql(studentPasswordHash)}, '演示学生${String(index).padStart(2, '0')}', 'STUDENT', ${sql(province)}, 2026, ${sql(subjectTrack)}, ${score}, ${rank}, '2026-09-${day} 10:00:00', '2026-09-${day} 10:00:00')`
  )
}

const universityRows = universities.map(([name, province, city, level, type], index) => {
  const id = index + 1
  return `(${id}, ${sql(name)}, ${sql(province)}, ${sql(city)}, ${sql(level)}, ${sql(type)}, ${sql('真实院校名称的演示条目，录取分数与位次均为虚构数据。')}, NULL, ${sql(`数据演示,${type},${level}`)})`
})

const majorRows = []
const admissionRows = []
const firstMajorByUniversity = new Map()
let majorId = 1
universities.forEach(([, , , , type, tier], universityIndex) => {
  const universityId = universityIndex + 1
  majorSets[type].forEach((majorName, majorIndex) => {
    const currentMajorId = majorId++
    if (majorIndex === 0) firstMajorByUniversity.set(universityId, currentMajorId)
    const category = categoryByName[majorName]
    const requirement = ['临床医学', '药学', '医学影像学', '预防医学'].includes(majorName)
      ? '物理+化学'
      : ['金融学', '会计学', '经济学', '法学', '汉语言文学', '教育学'].includes(majorName)
        ? '物理或历史+不限'
        : '物理+不限'
    majorRows.push(
      `(${currentMajorId}, ${universityId}, ${sql(majorName)}, ${sql(category)}, ${sql(requirement)}, ${sql(`用于系统演示的${majorName}专业条目。`)})`
    )

    const baseScore = tier === 1 ? 620 : tier === 2 ? 575 : 510
    const baseRank = tier === 1
      ? 5000
      : tier === 2
        ? 12000 + (universityIndex % 5) * 1500
        : 35000 + (universityIndex % 4) * 3000
    for (let yearOffset = 0; yearOffset < 3; yearOffset++) {
      const year = 2023 + yearOffset
      const minScore = baseScore + (universityIndex % 5) * 3 + yearOffset * 4 + majorIndex * 2
      const minRank = baseRank - yearOffset * 1800 - majorIndex * 400
      const planCount = 24 + (universityIndex % 12) * 2 + majorIndex * 3
      admissionRows.push(
        `(${universityId}, ${currentMajorId}, '山西', ${year}, '物理类', '本科批', ${minScore}, ${minRank}, ${planCount}, ${sql(source)}, '2026-10-15 09:00:00')`
      )
    }
  })
})

const favoriteRows = []
let favoriteId = 1
for (let userId = 2; userId <= 28; userId++) {
  const count = 2 + (userId % 3)
  const usedUniversities = new Set()
  for (let item = 0; item < count; item++) {
    let universityId = ((userId * 7 + item * 11) % universities.length) + 1
    while (usedUniversities.has(universityId)) {
      universityId = (universityId % universities.length) + 1
    }
    usedUniversities.add(universityId)
    const majorIdForFavorite = firstMajorByUniversity.get(universityId)
    const day = String((userId + item) % 28 + 1).padStart(2, '0')
    favoriteRows.push(
      `(${favoriteId++}, ${userId}, ${universityId}, ${majorIdForFavorite}, '2026-10-${day} 12:00:00')`
    )
  }
}

const output = `-- 本文件由 scripts/generate-demo-seed.mjs 生成。
-- 真实院校名称仅用于界面展示，所有分数、位次、招生计划和收藏关系均为虚构演示数据。
-- 直接重新导入本文件会清空并重建业务数据，仅用于开发、演示和论文测试环境。
USE gaokao_volunteer;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE favorite;
TRUNCATE TABLE admission_record;
TRUNCATE TABLE major;
TRUNCATE TABLE university;
TRUNCATE TABLE sys_user;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_user
  (id, username, password_hash, nickname, role, province, exam_year, subject_track, score, province_rank, created_at, updated_at)
VALUES
${users.join(',\n')};

INSERT INTO university
  (id, name, province, city, level, type, description, website, tags)
VALUES
${universityRows.join(',\n')};

INSERT INTO major
  (id, university_id, name, category, subject_requirement, description)
VALUES
${majorRows.join(',\n')};

INSERT INTO admission_record
  (university_id, major_id, province, year, subject_track, batch, min_score, min_rank, plan_count, source, created_at)
VALUES
${admissionRows.join(',\n')};

INSERT INTO favorite
  (id, user_id, university_id, major_id, created_at)
VALUES
${favoriteRows.join(',\n')};
`

writeFileSync(target, output, 'utf8')
console.log(JSON.stringify({
  universities: universities.length,
  majors: majorRows.length,
  admissionRecords: admissionRows.length,
  students: users.length - 1,
  favorites: favoriteRows.length,
  target
}, null, 2))
