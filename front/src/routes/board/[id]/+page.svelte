<script lang="ts">
  //@ts-ignore
  import Editor from '@toast-ui/editor';
  import '@toast-ui/editor/dist/toastui-editor.css';
  import rq from '$lib/rq/rq.svelte';
  import type { components } from '$lib/types/api/v1/schema';
  import { page } from '$app/stores';
  import ToastUiEditor from '$lib/components/ToastUiEditor.svelte';
  import { onMount } from 'svelte';

  let comments: components['schemas']['CommentDto'][] = $state();
  let bestComment: components['schemas']['CommentDto'][] = $state();
  let post: components['schemas']['PostDto'] = $state();
  let editor: Editor;
  let commentEditOpen: number | null = $state();
  let selectedReason;

  let postId = parseInt($page.params.id);

  let body: string | undefined = $state();
  let div: HTMLDivElement;

  let likedNum: number = $state(0);
  let likedByCurrentUser: Boolean = $state(false);

  let commentLikedNum: number[] = $state([]);
  let commentLikedByCurrentUser: Boolean[] = $state([]);
  let bestCommentLikedNum: number[] = $state([]);
  let bestCommentLikedByCurrentUser: Boolean[] = $state([]);

  let isLoaded = false;
  async function load() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');
    const responseComment = await rq.apiEndPoints().GET(`/api/v1/comments/{postId}`, {
      params: {
        path: {
          postId: parseInt($page.params.id)
        }
      }
    });

    comments = responseComment.data?.data!;
    commentLikedNum = comments.map((comment) => comment.voteCount!);
    commentLikedByCurrentUser = comments.map((comment) => comment.likedByCurrentUser!);
    console.log('comments:');
    console.log(comments);

    const responseBestComment = await rq.apiEndPoints().GET(`/api/v1/comments/{postId}/top`, {
      params: {
        path: {
          postId: parseInt($page.params.id)
        }
      }
    });
    bestComment = responseBestComment.data?.data!;
    bestCommentLikedByCurrentUser = bestComment.map((comment) => comment.likedByCurrentUser!);
    bestCommentLikedNum = bestComment.map((comment) => comment.voteCount!);

    const responsePost = await rq.apiEndPointsWithFetch(fetch).GET(`/api/v1/posts/{id}`, {
      params: {
        path: {
          id: parseInt($page.params.id)
        }
      }
    });
    post = responsePost.data?.data!;
    likedNum = post.voteCount;
    likedByCurrentUser = post.likedByCurrentUser;
    console.log(responsePost);
    return { comments, post };
  }

  onMount(() => {
    setTimeout(() => {
      console.log('hi');
      const fragmentIdentifier = window.location.hash.substring(1); // #을 제외한 fragment 식별자만 가져옴
      console.log(fragmentIdentifier); // "comment__5"
      const commentElement = document.getElementById(fragmentIdentifier); // 댓글 요소의 ID를 사용하여 찾음
      console.log(commentElement);
      // 댓글 요소가 있다면 스크롤을 해당 요소로 이동
      if (commentElement) {
        commentElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
        commentElement.classList.add('bg-blue-50');
        setTimeout(() => {
          commentElement.classList.remove('bg-blue-50');
        }, 700);
      }
    }, 100); // 1초(1000ms)의 지연 추가
  });

  async function deletePost() {
    const isConfirmed = confirm('게시물을 삭제하시겠습니까?');

    if (isConfirmed) {
      const { data, error } = await rq.apiEndPoints().DELETE(`/api/v1/posts/{id}`, {
        params: { path: { id: parseInt($page.params.id) } }
      });

      if (data) {
        rq.msgInfo(data.msg);
        rq.goTo('/board');
      } else if (error) {
        rq.msgError(error.msg);
      }
    }
  }

  async function deleteComment(comment: components['schemas']['CommentDto']) {
    const isConfirmed = confirm('댓글을 삭제하시겠습니까?');

    if (isConfirmed) {
      const { data, error } = await rq
        .apiEndPoints()
        .DELETE(`/api/v1/comments/{postId}/{commentId}`, {
          params: { path: { postId: parseInt($page.params.id), commentId: comment.id } }
        });

      if (data) {
        rq.msgInfo(data.msg);
        comments.splice(comments.indexOf(comment), 1);
      } else if (error) {
        rq.msgError(error.msg);
      }
    }
  }

  let modalreport;

  function openModalReport() {
    modalreport.showModal();
  }

  async function reportPost() {
    if (rq.isLogout()) {
      rq.msgError('로그인 후 이용 해 주세요');
      rq.goTo('/member/login');
    }

    if (selectedReason) {
      const { data, error } = await rq.apiEndPoints().POST(`/api/v1/report/post`, {
        body: {
          reportReason: selectedReason,
          materialId: parseInt($page.params.id)
        }
      });

      if (data) {
        rq.msgInfo('신고 되었습니다.');
        modalreport.close();
      }
    } else {
      rq.msgWarning('신고 사유를 선택 해 주세요');
    }
  }

  async function clickLikedPost() {
    if (likedByCurrentUser) {
      const { data, error } = await rq.apiEndPoints().DELETE(`/api/v1/posts/{id}/like`, {
        params: { path: { id: parseInt($page.params.id) } }
      });
      if (data) {
        rq.msgInfo('좋아요 취소');
        likedByCurrentUser = false;
        likedNum -= 1;
      } else if (error) {
        rq.msgError(error.msg);
      }
    } else {
      const { data, error } = await rq.apiEndPoints().POST(`/api/v1/posts/{id}/like`, {
        params: { path: { id: parseInt($page.params.id) } }
      });
      if (data) {
        rq.msgInfo('좋아요!!');
        likedByCurrentUser = true;

        likedNum += 1;
      } else if (error) {
        rq.msgError('로그인 후 이용 해 주세요');
        rq.goTo('/member/login');
      }
    }
  }

  async function clickLikedComment(comment: components['schemas']['CommentDto']) {
    if (commentLikedByCurrentUser[comments.indexOf(comment)]) {
      const { data, error } = await rq
        .apiEndPoints()
        .DELETE(`/api/v1/comments/{postId}/{commentId}/like`, {
          params: { path: { postId: parseInt($page.params.id), commentId: comment.id } }
        });
      if (data) {
        rq.msgInfo('좋아요 취소');

        let index = bestComment.findIndex((com) => com.id == comment.id);
        bestCommentLikedByCurrentUser[index] = !commentLikedByCurrentUser[index];
        bestCommentLikedNum[index] -= 1;

        commentLikedByCurrentUser[comments.indexOf(comment)] =
          !commentLikedByCurrentUser[comments.indexOf(comment)];
        commentLikedNum[comments.indexOf(comment)] -= 1;
      } else if (error) {
        rq.msgError(error.msg);
      }
    } else {
      const { data, error } = await rq
        .apiEndPoints()
        .POST(`/api/v1/comments/{postId}/{commentId}/like`, {
          params: { path: { postId: parseInt($page.params.id), commentId: comment.id } }
        });
      if (data) {
        rq.msgInfo('좋아요!!');
        let index = bestComment.findIndex((com) => com.id == comment.id);
        bestCommentLikedByCurrentUser[index] = !commentLikedByCurrentUser[index];
        bestCommentLikedNum[index] += 1;
        commentLikedByCurrentUser[comments.indexOf(comment)] =
          !commentLikedByCurrentUser[comments.indexOf(comment)];
        commentLikedNum[comments.indexOf(comment)] += 1;
      } else if (error) {
        rq.msgError('로그인 후 이용 해 주세요');
        rq.goTo('/member/login');
      }
    }
  }

  async function clickLikedBestComment(comment: components['schemas']['CommentDto']) {
    if (bestCommentLikedByCurrentUser[bestComment.indexOf(comment)]) {
      const { data, error } = await rq
        .apiEndPoints()
        .DELETE(`/api/v1/comments/{postId}/{commentId}/like`, {
          params: { path: { postId: parseInt($page.params.id), commentId: comment.id } }
        });
      if (data) {
        rq.msgInfo('좋아요 취소');

        let index = comments.findIndex((com) => com.id == comment.id);
        commentLikedByCurrentUser[index] = !commentLikedByCurrentUser[index];
        commentLikedNum[index] -= 1;
        bestCommentLikedByCurrentUser[bestComment.indexOf(comment)] =
          !bestCommentLikedByCurrentUser[bestComment.indexOf(comment)];
        bestCommentLikedNum[bestComment.indexOf(comment)] -= 1;
      } else if (error) {
        rq.msgError(error.msg);
      }
    } else {
      const { data, error } = await rq
        .apiEndPoints()
        .POST(`/api/v1/comments/{postId}/{commentId}/like`, {
          params: { path: { postId: parseInt($page.params.id), commentId: comment.id } }
        });
      if (data) {
        rq.msgInfo('좋아요!!');
        let index = comments.findIndex((com) => com.id == comment.id);
        commentLikedByCurrentUser[index] = !commentLikedByCurrentUser[index];
        commentLikedNum[index] += 1;
        bestCommentLikedByCurrentUser[bestComment.indexOf(comment)] =
          !bestCommentLikedByCurrentUser[bestComment.indexOf(comment)];
        bestCommentLikedNum[bestComment.indexOf(comment)] += 1;
      } else if (error) {
        rq.msgError('로그인 후 이용 해 주세요');
        rq.goTo('/member/login');
      }
    }
  }

  const Comment__save = async () => {
    const { data, error } = await rq.apiEndPointsWithFetch(fetch).POST('/api/v1/comments', {
      // url 설정
      body: {
        body: body,
        postId: postId
      }
    });

    if (data) {
      rq.msgInfo(data.msg); //msg
      location.reload();
      body = '';
    }
  };

  const fetchModiComment = async (commentid: number, commendbody: string) => {
    const { data, error } = await rq
      .apiEndPointsWithFetch(fetch)
      .PUT('/api/v1/comments/{postId}/{commentId}', {
        params: {
          path: {
            postId: parseInt($page.params.id),
            commentId: commentid
          }
        },
        // url 설정
        body: {
          body: commendbody,
          postId: postId
        }
      });

    if (data) {
      rq.msgInfo(data.msg); //msg
      commentEditOpen = null;
    }
  };
</script>

{#await load()}
  <h1>loading...</h1>
{:then { comments, post }}
  <div class="max-w-lg mx-auto w-full px-4 sm:px-6 lg:px-8">
    <div class="max-w-4xl mx-auto my-8">
      <div>
        <h1 class="text-3xl font-bold mb-4">{post.title}</h1>
      </div>

      <div class="flex">
        <div class="flex flex-1">
          <p class="text-nm space-y-1.5">
            {post.authorName} ·
            {(() => {
              const now = new Date();
              const postDate = new Date(post.createDate);
              const seconds = Math.floor((now - postDate) / 1000);

              let interval = seconds / 31536000;
              if (interval > 1) {
                return Math.floor(interval) + '년 전';
              }
              interval = seconds / 2592000;
              if (interval > 1) {
                return Math.floor(interval) + '개월 전';
              }
              interval = seconds / 86400;
              if (interval > 1) {
                return Math.floor(interval) + '일 전';
              }
              interval = seconds / 3600;
              if (interval > 1) {
                return Math.floor(interval) + '시간 전';
              }
              interval = seconds / 60;
              if (interval > 1) {
                return Math.floor(interval) + '분 전';
              }
              return Math.floor(seconds) + '초 전';
            })()}
          </p>
        </div>
        <div class="mb-2">
          <a onclick={openModalReport} class=""
            ><svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="black"
              class="w-8 h-8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
              />
            </svg>
          </a>
          <dialog id="my_modal_3" class="modal" bind:this={modalreport}>
            <div class="modal-box">
              <button
                class="mb-2 btn btn-sm btn-circle btn-ghost absolute right-2 top-2 focus:outline-none"
                on:click={() => modalreport.close()}>✕</button
              >
              <div class="flex flex-col p-6 bg-white shadow rounded-lg">
                <h2 class="text-xl font-semibold mb-4 pb-2">신고 사유 선택</h2>
                <form>
                  <div class="flex flex-col">
                    <label class="mb-2">
                      <input type="radio" name="reason" value="spam" bind:group={selectedReason} />
                      관련성 없는 홍보 및 도배
                    </label>
                    <label class="mb-2">
                      <input
                        type="radio"
                        name="reason"
                        value="violence"
                        bind:group={selectedReason}
                      />
                      성인물 · 폭력성
                    </label>
                    <label class="mb-2">
                      <input
                        type="radio"
                        name="reason"
                        value="demeaning"
                        bind:group={selectedReason}
                      />
                      타인 비하
                    </label>
                  </div>
                  <div class="flex justify-end mt-4">
                    <button
                      on:click={reportPost}
                      class="inline-block px-4 py-2 border border-gray-400 text-gray-700 bg-white hover:bg-gray-700 hover:text-white rounded-md shadow-sm text-sm font-medium focus:outline-none"
                    >
                      제출
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </dialog>
        </div>
      </div>

      <div>
        <div class="flex-1 flex justify-end items-center mb-5">
          <div class="flex">
            {#if rq.member.id == post.authorId || rq.isAdmin()}
              <div class="mb-5 mx-1 flex">
                <a
                  class="inline-block px-3 py-2 border border-gray-400 text-gray-700 bg-white hover:bg-gray-700 hover:text-white rounded-md shadow-sm text-sm font-medium focus:outline-none"
                  on:click={deletePost}>삭제</a
                >
              </div>
            {/if}
            {#if rq.member.id == post.authorId}
              <div class="mb-5 mx-1 flex">
                <a
                  href="/board/{post.id}/edit"
                  class="inline-block px-3 py-2 border border-gray-400 text-gray-700 bg-white hover:bg-gray-700 hover:text-white rounded-md shadow-sm text-sm font-medium focus:outline-none"
                  >수정</a
                >
              </div>
            {/if}
          </div>
        </div>
        <div class="flex-1 text-lg">
          <ToastUiEditor body={post.body} viewer={true}></ToastUiEditor>
        </div>
        <div class="flex flex-1 justify-center mt-20">
          <button
            class="btn btn-outline hover:bg-gray-100 hover:text-black flex-col h-14"
            on:click={clickLikedPost}
          >
            {#if likedByCurrentUser}
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="red"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="red"
                class="w-6 h-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                />
              </svg>
            {:else}
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-6 h-6"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                />
              </svg>
            {/if}
            <span>{likedNum}</span>
          </button>
        </div>
      </div>
      {#if rq.isLogin()}
        <div class="border-t mb-8 mt-10"></div>
        <div>
          <div class="mt-8 flex gap-2 items-center">
            <textarea
              id="commentbody"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-gray-600"
              rows="2"
              placeholder="댓글을 입력하세요..."
              bind:value={body}
            ></textarea>
            <button
              class="btn bg-white border-gray-400 hover:bg-gray-700 hover:text-white hover:outline-none focus:outline-none"
              on:click={Comment__save}>댓글 등록</button
            >
          </div>
        </div>
      {/if}

      <div class="border-t my-8"></div>
      <div>댓글 ({post.commentCount})</div>
      {#each bestComment as comment}
        <div class="rounded-md bg-yellow-50">
          <div class="border-b flex justify-between">
            <div>
              <div class="flex items-center">
                <div class="ml-5">
                  <span class="font-bold mr-2">{comment.authorName}</span>
                  <span
                    class="inline-flex px-2 font-semibold rounded-full mt-1 my-1 bg-red-100 text-red-800"
                    >Best</span
                  >
                </div>
                <div>
                  <p class="text-nm space-y-1.5 p-6">
                    {(() => {
                      const now = new Date();
                      const commentDate = new Date(comment.createDate);
                      const seconds = Math.floor((now - commentDate) / 1000);

                      let interval = seconds / 31536000;
                      if (interval > 1) {
                        return Math.floor(interval) + '년 전';
                      }
                      interval = seconds / 2592000;
                      if (interval > 1) {
                        return Math.floor(interval) + '개월 전';
                      }
                      interval = seconds / 86400;
                      if (interval > 1) {
                        return Math.floor(interval) + '일 전';
                      }
                      interval = seconds / 3600;
                      1;
                      if (interval > 1) {
                        return Math.floor(interval) + '시간 전';
                      }
                      interval = seconds / 60;
                      if (interval > 1) {
                        return Math.floor(interval) + '분 전';
                      }
                      return Math.floor(seconds) + '초 전';
                    })()}
                  </p>
                </div>
                <div class="flex justify-end flex gap-2 text-gray-400">
                  {#if rq.member.id == comment.authorId}
                    <button
                      class="text-xs"
                      on:click={() => {
                        commentEditOpen = comment.id;
                      }}>수정</button
                    >
                    <p>/</p>
                  {/if}
                  {#if rq.member.id == comment.authorId || rq.isAdmin()}
                    <div>
                      <button class="text-xs" on:click={() => deleteComment(comment)}>삭제</button>
                    </div>
                  {/if}
                </div>
              </div>
              <div class="flex items-center mx-5 mb-5">
                <span class="text-gray-600">{comment.body}</span>
              </div>
            </div>
            <div class="flex items-center mr-5">
              <button
                class="btn btn-outline hover:bg-gray-100 hover:text-black flex-col h-14"
                on:click={() => clickLikedBestComment(comment)}
              >
                {#if bestCommentLikedByCurrentUser[bestComment.indexOf(comment)]}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="red"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="red"
                    class="w-6 h-6"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                    />
                  </svg>
                {:else}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="w-6 h-6"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                    />
                  </svg>
                {/if}
                <span>{bestCommentLikedNum[bestComment.indexOf(comment)]}</span>
              </button>
            </div>
          </div>
        </div>
        {#if commentEditOpen == comment.id}
          <div class="flex justify-center items-center">
            <textarea
              class="w-full px-3 py-2 h-20 border rounded-md focus:outline-none focus:border-blue-500 mt-4 mx-2"
              rows="4"
              bind:value={comment.body}
            ></textarea>

            <button class="btn" on:click={() => fetchModiComment(comment.id, comment.body)}
              >저장</button
            >
          </div>
        {/if}
      {/each}

      {#each comments as comment}
        <div id="comment__{comment.id}" class="">
          <div class="border-b rounded-sm flex justify-between">
            <div>
              <div class="flex items-center">
                <div class="ml-5">
                  <span class="font-bold mr-2">{comment.authorName}</span>
                </div>
                <div>
                  <p class="text-nm space-y-1.5 p-6">
                    {(() => {
                      const now = new Date();
                      const commentDate = new Date(comment.createDate);
                      const seconds = Math.floor((now - commentDate) / 1000);

                      let interval = seconds / 31536000;
                      if (interval > 1) {
                        return Math.floor(interval) + '년 전';
                      }
                      interval = seconds / 2592000;
                      if (interval > 1) {
                        return Math.floor(interval) + '개월 전';
                      }
                      interval = seconds / 86400;
                      if (interval > 1) {
                        return Math.floor(interval) + '일 전';
                      }
                      interval = seconds / 3600;
                      1;
                      if (interval > 1) {
                        return Math.floor(interval) + '시간 전';
                      }
                      interval = seconds / 60;
                      if (interval > 1) {
                        return Math.floor(interval) + '분 전';
                      }
                      return Math.floor(seconds) + '초 전';
                    })()}
                  </p>
                </div>
              </div>
              <div class="flex items-center mx-5 mb-5">
                <span class="text-gray-600">{comment.body}</span>
              </div>
            </div>
            <div class="flex flex-col items-center mr-5 mt-4">
              <button
                class="btn btn-outline hover:bg-gray-100 hover:text-black flex-col h-14"
                on:click={() => clickLikedComment(comment)}
              >
                {#if commentLikedByCurrentUser[comments.indexOf(comment)]}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="red"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="red"
                    class="w-6 h-6"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                    />
                  </svg>
                {:else}
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke-width="1.5"
                    stroke="currentColor"
                    class="w-6 h-6"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z"
                    />
                  </svg>
                {/if}
                <span>{commentLikedNum[comments.indexOf(comment)]}</span>
              </button>
              <div class="flex justify-end flex gap-2 mt-2 text-gray-400">
                {#if rq.member.id == comment.authorId}
                  <button
                    class="text-xs"
                    on:click={() => {
                      commentEditOpen = comment.id;
                    }}>수정</button
                  >
                  <p>/</p>
                {/if}
                {#if rq.member.id == comment.authorId || rq.isAdmin()}
                  <div>
                    <button class="text-xs" on:click={() => deleteComment(comment)}>삭제</button>
                  </div>
                {/if}
              </div>
            </div>
          </div>
        </div>
        {#if commentEditOpen == comment.id}
          <div class="flex justify-center items-center">
            <textarea
              class="w-full px-3 py-2 h-20 border rounded-md focus:outline-none focus:border-blue-500 mt-4 mx-2"
              rows="4"
              bind:value={comment.body}
            ></textarea>

            <button class="btn" on:click={() => fetchModiComment(comment.id, comment.body)}
              >저장</button
            >
          </div>
        {/if}
      {/each}
    </div>
  </div>
{/await}

<style>
  /* 애니메이션 효과 추가 */
  .bg-blue-200 {
    transition: background-color 0.5s ease;
  }
</style>
