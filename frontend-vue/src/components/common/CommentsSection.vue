<template>
  <div class="comments-section">
    <div class="comments-header">
      <h4><MessageOutlined /> 评论 ({{ comments.length }})</h4>
      <a-button type="link" size="small" @click="showCommentInput = !showCommentInput">
        <PlusOutlined /> 添加评论
      </a-button>
    </div>

    <!-- Comment Input -->
    <div v-if="showCommentInput" class="comment-input">
      <a-textarea v-model:value="newComment" :rows="3" placeholder="添加评论或批注..." />
      <div class="comment-input-actions">
        <a-space>
          <a-button size="small" @click="showCommentInput = false">取消</a-button>
          <a-button type="primary" size="small" @click="submitComment" :loading="submitting" :disabled="!newComment.trim()">
            发布
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- Comments List -->
    <div class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <a-avatar :style="{ background: comment.color }" size="small">
          {{ comment.author[0] }}
        </a-avatar>
        <div class="comment-body">
          <div class="comment-meta">
            <span class="comment-author">{{ comment.author }}</span>
            <span class="comment-time">{{ comment.time }}</span>
            <a-tag v-if="comment.mention" color="blue" size="small">@{{ comment.mention }}</a-tag>
          </div>
          <div class="comment-text">{{ comment.text }}</div>
          <div class="comment-actions">
            <a-button type="link" size="small" @click="replyTo(comment)">回复</a-button>
            <a-button type="link" size="small" @click="likeComment(comment)">
              <LikeOutlined /> {{ comment.likes || '' }}
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-if="comments.length === 0" description="暂无评论" :image-style="{ height: '30px' }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { MessageOutlined, PlusOutlined, LikeOutlined } from '@ant-design/icons-vue'
import { post, get } from '@/api/request'

interface Comment {
  id: string
  author: string
  text: string
  time: string
  color: string
  mention?: string
  likes?: number
}

const props = defineProps<{
  refType: string
  refId: string
}>()

const showCommentInput = ref(false)
const newComment = ref('')
const submitting = ref(false)
const comments = ref<Comment[]>([])

const colors = ['#1677ff', '#52c41a', '#722ed1', '#fa8c16', '#eb2f96', '#13c2c2']

async function loadComments() {
  try {
    const res = await get<{ data: Comment[] }>(`/comments/${props.refType}/${props.refId}`)
    comments.value = res?.data || []
  } catch {
    // Demo comments
    comments.value = [
      { id: '1', author: '张三', text: '这个报表的销售数据和我们CRM系统对不上，需要核实一下数据源。', time: '2小时前', color: colors[0], likes: 2 },
      { id: '2', author: '李四', text: '@张三 已核实，是数据同步延迟导致的差异，下次同步后会一致。', time: '1小时前', color: colors[1], mention: '张三' },
    ]
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    await post(`/comments/${props.refType}/${props.refId}`, { text: newComment.value })
    comments.value.push({
      id: Date.now().toString(),
      author: '当前用户',
      text: newComment.value,
      time: '刚刚',
      color: colors[Math.floor(Math.random() * colors.length)],
    })
    newComment.value = ''
    showCommentInput.value = false
    message.success('评论已发布')
  } catch {
    message.error('发布失败')
  } finally {
    submitting.value = false
  }
}

function replyTo(comment: Comment) {
  newComment.value = `@${comment.author} `
  showCommentInput.value = true
}

function likeComment(comment: Comment) {
  comment.likes = (comment.likes || 0) + 1
}

loadComments()
</script>

<style lang="scss" scoped>
.comments-section {
  margin-top: 16px;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h4 {
    margin: 0;
    font-size: 14px;
    color: #333;
  }
}

.comment-input {
  margin-bottom: 16px;

  .comment-input-actions {
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
  }
}

.comments-list {
  max-height: 400px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child { border-bottom: none; }
}

.comment-body {
  flex: 1;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;

  .comment-author {
    font-weight: 600;
    font-size: 13px;
    color: #333;
  }

  .comment-time {
    font-size: 11px;
    color: #999;
  }
}

.comment-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.comment-actions {
  margin-top: 4px;
}
</style>
